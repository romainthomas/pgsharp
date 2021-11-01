public class SignatureMock implements InvocationHandler {
  public String mOriginalSignature;
  public String mPackageName = "";
  public Object mPackageManager;

  public SignatureMock(Object pm, String originalSignature, String packageName) {
    this.mPackageManager = pm;
    this.mOriginalSignature = originalSignature;
    this.mPackageName = packageName;
  }

  @Override // java.lang.reflect.InvocationHandler
  public Object invoke(Object obj, Method inMeth, Object[] args) {
    PackageInfo packageInfo;
    SigningInfo signingInfo;

    // Hook getPackageInfo
    if ("getPackageInfo".equals(inMeth.getName())) {
      String pkgName = (String) args[0];
      int flags = ((Integer) args[1]).intValue();

      // Handle both
      // GET_SIGNATURES           (0x00000040) - Deprecated in API 28
      // GET_SIGNING_CERTIFICATES (0x08000000)
      if ((flags & PackageManager.GET_SIGNATURES) != 0 && this.mPackageName.equals(pkgName)) {
        PackageInfo fakePkgInfo = (PackageInfo) inMeth.invoke(this.mPackageManager, args);

        // Fake the signature
        fakePkgInfo.signatures[0] = new Signature(this.mOriginalSignature);
        return fakePkgInfo;
      } else if (Build.VERSION.SDK_INT >= 28 &&
                (flags & GET_SIGNING_CERTIFICATES) != 0 &&
                this.mPackageName.equals(pkgName) &&
                (signingInfo = (packageInfo = (PackageInfo) method.invoke(this.mPackageManager, args)).signingInfo) != null) {
        Field FieldSigningDetails = signingInfo.getClass().getDeclaredField("mSigningDetails");
        FieldSigningDetails.setAccessible(true);

        Object mSigningDetails = FieldSigningD.get(packageInfo);
        Signature[] fakeSigArray = {new Signature(this.mOriginalSignature)};
        Field FieldSignatures = mSigningDetails.getClass().getDeclaredField("signatures");
        FieldSignatures.setAccessible(true);
        FieldSignatures.set(FieldSigningDetails, fakeSigArray);
        return packageInfo;
      }
    }
    return inMeth.invoke(this.mPackageManager, args);
  }
}
public static void proxifySignatureCheck(Context context) {
  String packageName = context.getPackageName();
  Class<?> aThreadCls = Class.forName("android.app.ActivityThread");
  Object mCurrentActivityThread = aThreadCls.getDeclaredMethod("currentActivityThread", new Class[0]).invoke(null, new Object[0]);

  Field sPackageManager = aThreadCls.getDeclaredField("sPackageManager");
  sPackageManager.setAccessible(true);

  Object pm = sPackageManager.get(mCurrentActivityThread);
  Class<?> IPackageManager = Class.forName("android.content.pm.IPackageManager");
  SignatureMock mock = new SignatureMock(pm, "30820 [ ... ] aa001f55", packageName)
  Object newProxyInstance = Proxy.newProxyInstance(IPackageManager.getClassLoader(), new Class[]{IPackageManager}, mock);
  sPackageManager.set(mCurrentActivityThread, newProxyInstance);
  PackageManager packageManager = context.getPackageManager();
  Field mPM = packageManager.getClass().getDeclaredField("mPM");
  mPM.setAccessible(true);
  mPM.set(packageManager, newProxyInstance);
}


