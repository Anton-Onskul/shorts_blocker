package com.shortsblock.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Отдаёт один файл — отчёт диагностики — приложению, выбранному в «Поделиться».
 * Отчёт бывает больше лимита Intent (~1 МБ), поэтому передаём файлом, а не текстом.
 * Провайдер не экспортирован: доступ только по временному разрешению на конкретный URI.
 */
public final class ReportProvider extends ContentProvider {
    static final String AUTHORITY = "com.shortsblock.app.report";
    static final String FILE_NAME = "shorts-blocker-report.txt";
    static final Uri URI = Uri.parse("content://" + AUTHORITY + "/" + FILE_NAME);

    static File file(android.content.Context c) {
        return new File(c.getCacheDir(), FILE_NAME);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!URI.equals(uri) || getContext() == null) {
            throw new FileNotFoundException(String.valueOf(uri));
        }
        return ParcelFileDescriptor.open(file(getContext()), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        if (URI.equals(uri) && getContext() != null) {
            c.addRow(new Object[]{FILE_NAME, file(getContext()).length()});
        }
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return "text/plain";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
