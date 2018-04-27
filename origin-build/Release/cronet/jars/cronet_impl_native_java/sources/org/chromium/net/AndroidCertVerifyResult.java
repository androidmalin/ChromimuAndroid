package org.chromium.net;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("net::android")
public class AndroidCertVerifyResult {
    private final List<X509Certificate> mCertificateChain;
    private final boolean mIsIssuedByKnownRoot;
    private final int mStatus;

    public AndroidCertVerifyResult(int status, boolean isIssuedByKnownRoot, List<X509Certificate> certificateChain) {
        this.mStatus = status;
        this.mIsIssuedByKnownRoot = isIssuedByKnownRoot;
        this.mCertificateChain = new ArrayList(certificateChain);
    }

    public AndroidCertVerifyResult(int status) {
        this.mStatus = status;
        this.mIsIssuedByKnownRoot = false;
        this.mCertificateChain = Collections.emptyList();
    }

    @CalledByNative
    public int getStatus() {
        return this.mStatus;
    }

    @CalledByNative
    public boolean isIssuedByKnownRoot() {
        return this.mIsIssuedByKnownRoot;
    }

    @CalledByNative
    public byte[][] getCertificateChainEncoded() {
        byte[][] verifiedChainArray = new byte[this.mCertificateChain.size()][];
        int i = 0;
        while (i < this.mCertificateChain.size()) {
            try {
                verifiedChainArray[i] = ((X509Certificate) this.mCertificateChain.get(i)).getEncoded();
                i++;
            } catch (CertificateEncodingException e) {
                return new byte[0][];
            }
        }
        return verifiedChainArray;
    }
}
