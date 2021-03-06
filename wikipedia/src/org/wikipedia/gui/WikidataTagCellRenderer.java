// License: GPL. For details, see LICENSE file.
package org.wikipedia.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.data.WikidataEntry;

public class WikidataTagCellRenderer extends DefaultTableCellRenderer {

    final Map<String, CompletableFuture<String>> labelCache = new ConcurrentHashMap<>();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (column != 1
                || !(value instanceof Map<?, ?> && ((Map<?, ?>) value).size() == 1)) {
            return null;
        }
        final String key = table.getValueAt(row, 0).toString();
        if (!("wikidata".equals(key) || (key != null && key.endsWith(":wikidata")))) {
            return null;
        }

        final String id = ((Map<?, ?>) value).keySet().iterator().next().toString();
        final JLabel component = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (WikipediaApp.WIKIDATA_PATTERN.matcher(id).matches()) {
            return renderValues(Collections.singleton(id), table, component);
        } else if (id.contains(";")) {
            final List<String> ids = Arrays.asList(id.split("\\s*;\\s*"));
            if (ids.stream().allMatch(i -> WikipediaApp.WIKIDATA_PATTERN.matcher(i).matches())) {
                return renderValues(ids, table, component);
            }
        }
        return null;
    }

    protected JLabel renderValues(Collection<String> ids, JTable table, JLabel component) {

        ids.forEach(id ->
                labelCache.computeIfAbsent(id, x ->
                        CompletableFuture.supplyAsync(() -> WikipediaApp.getLabelForWikidata(x, Locale.getDefault())))
        );

        final Collection<String> texts = new ArrayList<>(ids.size());
        for (String id : ids) {
            if (!labelCache.get(id).isDone()) {
                labelCache.get(id).thenRun(() -> GuiHelper.runInEDT(table::repaint));
                return null;
            }
            final String label;
            try {
                label = labelCache.get(id).get();
            } catch (InterruptedException | ExecutionException e) {
                Main.warn("Could not fetch Wikidata label for " + id);
                Main.warn(e);
                return null;
            }
            if (label == null) {
                return null;
            }
            texts.add(WikidataEntry.getLabelText(id, label));
        }
        component.setText("<html>" + texts.stream().collect(Collectors.joining("; ")));
        component.setToolTipText("<html>" + Utils.joinAsHtmlUnorderedList(texts));
        return component;
    }
}
