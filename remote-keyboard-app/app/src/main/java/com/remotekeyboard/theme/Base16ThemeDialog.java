package com.remotekeyboard.theme;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo interactivo para explorar, buscar y aplicar cualquiera de los 360+ esquemas Base16.
 */
public class Base16ThemeDialog {

    public interface OnThemeSelectedListener {
        void onThemeSelected(Base16Theme theme);
    }

    public static void show(Context context, OnThemeSelectedListener listener) {
        Base16ThemeManager manager = Base16ThemeManager.getInstance();
        manager.ensureLoaded(context);

        List<Base16Theme> initialList = new ArrayList<>(manager.getAllThemes(context));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Layout contenedor principal
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 24, 32, 16);
        container.setBackgroundColor(Color.parseColor("#090D16"));

        // Header
        TextView tvHeader = new TextView(context);
        tvHeader.setText("🎨 Esquemas Base16 (" + initialList.size() + ")");
        tvHeader.setTextSize(18);
        tvHeader.setTextColor(Color.WHITE);
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(tvHeader);

        TextView tvSub = new TextView(context);
        tvSub.setText("Toca para aplicar a tu teclado al instante:");
        tvSub.setTextSize(12);
        tvSub.setTextColor(Color.parseColor("#94A3B8"));
        tvSub.setPadding(0, 4, 0, 16);
        container.addView(tvSub);

        // Campo de búsqueda
        EditText etSearch = new EditText(context);
        etSearch.setHint("Buscar tema (ej: nord, dracula, gruvbox)...");
        etSearch.setHintTextColor(Color.parseColor("#64748B"));
        etSearch.setTextColor(Color.WHITE);
        etSearch.setTextSize(14);
        etSearch.setPadding(24, 20, 24, 20);

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.parseColor("#162035"));
        searchBg.setCornerRadius(16);
        searchBg.setStroke(2, Color.parseColor("#263859"));
        etSearch.setBackground(searchBg);
        container.addView(etSearch);

        // Lista de temas
        ListView listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(12);
        listView.setPadding(0, 16, 0, 8);

        ThemeAdapter adapter = new ThemeAdapter(context, initialList);
        listView.setAdapter(adapter);

        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                800 // altura fija scrollable
        );
        listParams.topMargin = 16;
        container.addView(listView, listParams);

        builder.setView(container);
        builder.setNegativeButton("Cerrar", (d, w) -> d.dismiss());

        AlertDialog dialog = builder.create();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<Base16Theme> filtered = manager.searchThemes(context, s.toString());
                adapter.updateData(filtered);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Base16Theme selected = adapter.getItem(position);
            if (selected != null) {
                manager.setSelectedTheme(context, selected);
                if (listener != null) {
                    listener.onThemeSelected(selected);
                }
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private static class ThemeAdapter extends BaseAdapter {
        private final Context context;
        private List<Base16Theme> items;

        public ThemeAdapter(Context context, List<Base16Theme> items) {
            this.context = context;
            this.items = items;
        }

        public void updateData(List<Base16Theme> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Base16Theme getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            if (convertView instanceof LinearLayout) {
                row = (LinearLayout) convertView;
            } else {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(20, 16, 20, 16);

                GradientDrawable rowBg = new GradientDrawable();
                rowBg.setColor(Color.parseColor("#121929"));
                rowBg.setCornerRadius(12);
                row.setBackground(rowBg);
            }

            row.removeAllViews();
            Base16Theme theme = getItem(position);

            // Columna de textos
            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            textCol.setLayoutParams(textParams);

            TextView tvName = new TextView(context);
            tvName.setText(theme.getName());
            tvName.setTextSize(14);
            tvName.setTextColor(Color.WHITE);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            textCol.addView(tvName);

            TextView tvAuthor = new TextView(context);
            tvAuthor.setText(theme.getVariant().toUpperCase() + " • " + theme.getAuthor());
            tvAuthor.setTextSize(11);
            tvAuthor.setTextColor(Color.parseColor("#64748B"));
            textCol.addView(tvAuthor);

            row.addView(textCol);

            // Muestra visual de paleta (4 píldoras de color)
            LinearLayout swatchRow = new LinearLayout(context);
            swatchRow.setOrientation(LinearLayout.HORIZONTAL);
            swatchRow.setGravity(Gravity.CENTER_VERTICAL);

            int[] swatchColors = new int[]{
                    theme.getBackgroundColor(),
                    theme.getKeyBackgroundColor(),
                    theme.getAccentColor(),
                    theme.getTextColor()
            };

            for (int color : swatchColors) {
                View dot = new View(context);
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(24, 24);
                dotParams.leftMargin = 6;
                dot.setLayoutParams(dotParams);

                GradientDrawable dotBg = new GradientDrawable();
                dotBg.setShape(GradientDrawable.OVAL);
                dotBg.setColor(color);
                dotBg.setStroke(1, Color.parseColor("#334155"));
                dot.setBackground(dotBg);

                swatchRow.addView(dot);
            }

            row.addView(swatchRow);

            return row;
        }
    }
}
