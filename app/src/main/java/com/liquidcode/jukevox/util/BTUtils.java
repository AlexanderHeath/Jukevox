package com.liquidcode.jukevox.util;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;

public interface BTUtils {

	// max connected clients allowed
	int MAX_BT_CLIENTS = 8;
	int MAX_SOCKET_READ = 8192; //2048;
}
