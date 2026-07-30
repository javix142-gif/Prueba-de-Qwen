package cl.javier.exploradorarchivos;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PICK_FOLDER = 100;
    private final List<FileItem> items = new ArrayList<>();
    private Uri treeUri;
    private String currentDocumentId;
    private String rootDocumentId;
    private TextView pathView;
    private ArrayAdapter<FileItem> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        String saved = getPreferences(MODE_PRIVATE).getString("tree_uri", null);
        if (saved != null) {
            try {
                treeUri = Uri.parse(saved);
                rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
                currentDocumentId = rootDocumentId;
                loadDirectory();
            } catch (Exception ignored) {
                chooseFolder();
            }
        } else {
            chooseFolder();
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("Explorador de archivos");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 0, 0, 16);
        root.addView(title);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button choose = new Button(this);
        choose.setText("Elegir carpeta");
        choose.setOnClickListener(v -> chooseFolder());
        actions.addView(choose, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button up = new Button(this);
        up.setText("Subir");
        up.setOnClickListener(v -> goUp());
        actions.addView(up, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(actions);

        pathView = new TextView(this);
        pathView.setText("Selecciona una carpeta para comenzar");
        pathView.setPadding(8, 16, 8, 16);
        root.addView(pathView);

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<FileItem>(this, android.R.layout.simple_list_item_2, android.R.id.text1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                FileItem item = getItem(position);
                TextView line1 = view.findViewById(android.R.id.text1);
                TextView line2 = view.findViewById(android.R.id.text2);
                line1.setText((item.directory ? "📁 " : "📄 ") + item.name);
                line2.setText(item.directory ? "Carpeta" : item.mimeType);
                return view;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> openItem(items.get(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            FileItem item = items.get(position);
            if (!item.directory) shareItem(item);
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView hint = new TextView(this);
        hint.setText("Toca para abrir. Mantén presionado un archivo para compartirlo.");
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(4, 16, 4, 4);
        root.addView(hint);

        setContentView(root);
    }

    private void chooseFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, PICK_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            treeUri = data.getData();
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try { getContentResolver().takePersistableUriPermission(treeUri, flags); } catch (SecurityException ignored) {}
            getPreferences(MODE_PRIVATE).edit().putString("tree_uri", treeUri.toString()).apply();
            rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
            currentDocumentId = rootDocumentId;
            loadDirectory();
        }
    }

    private void loadDirectory() {
        items.clear();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocumentId);
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
        };
        try (Cursor cursor = getContentResolver().query(childrenUri, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    String name = cursor.getString(1);
                    String mime = cursor.getString(2);
                    long size = cursor.isNull(3) ? 0 : cursor.getLong(3);
                    items.add(new FileItem(id, name == null ? "Sin nombre" : name, mime, size));
                }
            }
            Collections.sort(items, Comparator.comparing((FileItem f) -> !f.directory).thenComparing(f -> f.name.toLowerCase()));
            adapter.notifyDataSetChanged();
            pathView.setText("Carpeta: " + currentDocumentId + "  •  " + items.size() + " elementos");
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo leer la carpeta: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openItem(FileItem item) {
        if (item.directory) {
            currentDocumentId = item.documentId;
            loadDirectory();
            return;
        }
        Uri uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, item.documentId);
        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, item.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(intent); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "No hay una app compatible para abrir este archivo.", Toast.LENGTH_LONG).show(); }
    }

    private void shareItem(FileItem item) {
        Uri uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, item.documentId);
        Intent share = new Intent(Intent.ACTION_SEND).setType(item.mimeType).putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Compartir archivo"));
    }

    private void goUp() {
        if (treeUri == null || currentDocumentId == null || currentDocumentId.equals(rootDocumentId)) return;
        int slash = currentDocumentId.lastIndexOf('/');
        if (slash > 0) currentDocumentId = currentDocumentId.substring(0, slash);
        else currentDocumentId = rootDocumentId;
        loadDirectory();
    }

    private static class FileItem {
        final String documentId;
        final String name;
        final String mimeType;
        final long size;
        final boolean directory;

        FileItem(String documentId, String name, String mimeType, long size) {
            this.documentId = documentId;
            this.name = name;
            this.mimeType = mimeType == null ? "application/octet-stream" : mimeType;
            this.size = size;
            this.directory = DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
        }

        @Override public String toString() { return name; }
    }
}
