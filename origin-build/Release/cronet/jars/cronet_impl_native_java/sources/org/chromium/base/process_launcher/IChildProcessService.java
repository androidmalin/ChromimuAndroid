package org.chromium.base.process_launcher;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IChildProcessService extends IInterface {

    public static abstract class Stub extends Binder implements IChildProcessService {
        private static final String DESCRIPTOR = "org.chromium.base.process_launcher.IChildProcessService";
        static final int TRANSACTION_bindToCaller = 1;
        static final int TRANSACTION_crashIntentionallyForTesting = 3;
        static final int TRANSACTION_setupConnection = 2;

        private static class Proxy implements IChildProcessService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public boolean bindToCaller() throws RemoteException {
                boolean _result = true;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setupConnection(Bundle args, ICallbackInt pidCallback, List<IBinder> clientInterfaces) throws RemoteException {
                IBinder iBinder = null;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (pidCallback != null) {
                        iBinder = pidCallback.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    _data.writeBinderList(clientInterfaces);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void crashIntentionallyForTesting() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IChildProcessService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IChildProcessService)) {
                return new Proxy(obj);
            }
            return (IChildProcessService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = bindToCaller();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                case 2:
                    Bundle _arg0;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    setupConnection(_arg0, org.chromium.base.process_launcher.ICallbackInt.Stub.asInterface(data.readStrongBinder()), data.createBinderArrayList());
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    crashIntentionallyForTesting();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    boolean bindToCaller() throws RemoteException;

    void crashIntentionallyForTesting() throws RemoteException;

    void setupConnection(Bundle bundle, ICallbackInt iCallbackInt, List<IBinder> list) throws RemoteException;
}
