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
	int MAX_SOCKET_READ = 10240; // Server needs to be able to read more bytes at a time
    int MAX_CLIENT_SOCKET_READ = 256; // Client should be okay with this amount
    int SONG_CHUNK_SIZE = 10000; // 8000 bytes at a time?
}
