package jadx.core.dex.visitors.deobf;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.codegen.TypeGen;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;

import jadx.core.utils.InsnList;
import jadx.core.utils.InsnRemover;

import jadx.core.utils.exceptions.JadxRuntimeException;
import java.util.Base64;


public class PGSharpStrings extends AbstractVisitor {

  // To be adjusted with the PGSharp version
  private static final String ENCODING_METH = new String("h.a.a.r3");
  private static final String ENCODING_KEY  = new String("vqGqQWCVnDRrNXTR");

  private static final Logger LOG = LoggerFactory.getLogger(PGSharp.class);

  public static byte[] decode(byte[] bArr) {
    char[] KEY = ENCODING_KEY.toCharArray();
    byte[] bArr2 = new byte[bArr.length];
    for (int i = 0; i < bArr.length; i++) {
      byte c = (byte)(KEY[i % ENCODING_KEY.length()] & 0xFF);
      bArr2[i] = (byte) (bArr[i] ^ c);
    }
    return bArr2;
  }

  public static String decodeStr(String str) {
    byte[] decodedBytes = Base64.getDecoder().decode(str);
    return new String(decode(decodedBytes));
  }


  @Override
  public void visit(MethodNode mth) {
    LOG.info("Process: {}", mth.toString());
    if (mth.isNoCode()) {
      return;
    }
    for (BlockNode block : mth.getBasicBlocks()) {
      simplify(mth, block);
    }
  }

  private static String getConstString(InsnArg arg) {
    if (arg.isLiteral()) {
      return TypeGen.literalToRawString((LiteralArg) arg);
    }
    if (arg.isInsnWrap()) {
      InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
      if (wrapInsn instanceof ConstStringNode) {
        return ((ConstStringNode) wrapInsn).getString();
      }
    }
    return null;
  }


  private static InsnNode simplify(MethodNode mth, InsnNode insn) {
    boolean changed = false;
    for (InsnArg arg : insn.getArguments()) {
      if (arg.isInsnWrap()) {
        InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
        InsnNode ni = simplify(mth, wrapInsn);
        if (ni != null) {
          arg.wrapInstruction(mth, ni);
          InsnRemover.unbindInsn(mth, wrapInsn);
          changed = true;
        }
      }
    }

    if (changed) {
      insn.rebindArgs();
    }

    if (insn.getType() != InsnType.INVOKE) {
      return null;
    }

    MethodInfo callMth = ((InvokeNode) insn).getCallMth();
    if (callMth.getDeclClass().getFullName().equals(ENCODING_METH) && callMth.getName().equals("a")) {
      InsnArg arg0 = insn.getArg(0);
      LOG.info("simplify({})", arg0.toString());

      if (arg0.isInsnWrap()) {
        InsnNode wrapInsn = ((InsnWrapArg) arg0).getWrapInsn();
        String encoded = getConstString(arg0);
        if (encoded == null) {
          LOG.info("Can't get string object from {}", wrapInsn.toString());
          return null;
        }
        String decoded = decodeStr(encoded);

        LOG.info("{} --> {}", encoded, decoded);

        ConstStringNode constStrInsn = new ConstStringNode(decodeStr(encoded));
        return constStrInsn;
      }

      if (arg0.isRegister()) {
        LOG.info("Reg is not supported for {}", arg0.toString());
        return null;
      }
      return null;
    }

    return null;

  }

  private static void simplify(MethodNode mth, BlockNode block) {
    List<InsnNode> insns = block.getInstructions();
    int size = insns.size();
    for (int i = 0; i < size; i++) {
      InsnNode insn = insns.get(i);
      InsnNode modInsn = simplify(mth, insn);
      if (modInsn != null) {
        modInsn.setResult(insn.getResult());
        modInsn.rebindArgs();
        LOG.info("Simplify {} -> {}", insn.toString(), modInsn.toString());
        if (i < insns.size() && insns.get(i) == insn) {
          insns.set(i, modInsn);
        } else {
          int idx = InsnList.getIndex(insns, insn);
          if (idx == -1) {
            throw new JadxRuntimeException("Failed to replace insn");
          }
          insns.set(idx, modInsn);
        }
      }
    }
  }
}
