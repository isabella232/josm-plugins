// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.plugins.pt_assistant.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.tools.UserCancelException;

/**
 * Sorts the stop positions in a PT route according to the assigned ways
 *
 * @author Polyglot
 *
 */
public class CreatePlatformNodeAction extends JosmAction {

    private static final String ACTION_NAME = "Transfer details of stop to platform node";

    private Node dummy1;
    private Node dummy2;
    private Node dummy3;

    /**
     * Creates a new PlatformAction
     */
    public CreatePlatformNodeAction() {
        super(ACTION_NAME, "icons/sortptstops", ACTION_NAME, null, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> selection = getLayerManager().getEditDataSet().getSelected();
        Node platformNode = null;
        Node stopPositionNode = null;
        Way platformWay = null;

        for (OsmPrimitive item: selection) {
            if (item.getType() == OsmPrimitiveType.NODE) {
                if (item.hasTag("public_transport", "stop_position"))
                    stopPositionNode = (Node) item;
                else
                    platformNode = (Node) item;
            } else if (item.getType() == OsmPrimitiveType.WAY &&
                    item.hasTag("public_transport", "platform")) {
                platformWay = (Way) item;
            }
        }

        if (platformNode == null || stopPositionNode == null) {
            return;
        }

        dummy1 = new Node(platformNode.getEastNorth());
        dummy2 = new Node(platformNode.getEastNorth());
        dummy3 = new Node(platformNode.getEastNorth());

        Main.main.undoRedo.add(new AddCommand(dummy1));
        Main.main.undoRedo.add(new AddCommand(dummy2));
        Main.main.undoRedo.add(new AddCommand(dummy3));

        populateMap(stopPositionNode);
        populateMap(platformNode);

        if (platformWay != null) {
            populateMap(platformWay);
            platformWay.removeAll();
            platformWay.put("public_transport", "platform");
            platformWay.put(" highway", "platform");
        }

        stopPositionNode.removeAll();
        stopPositionNode.put("bus", "yes");
        stopPositionNode.put("public_transport", "stop_position");

        platformNode.removeAll();
        platformNode.put("public_transport", "platform");
        platformNode.put("highway", "bus_stop");

        List<OsmPrimitive> prims = new ArrayList<>();
        prims.add(platformNode);
        prims.add(dummy1);
        prims.add(dummy2);
        prims.add(dummy3);

        try {
            TagCollection tagColl = TagCollection.unionOfAllPrimitives(prims);
            List<Command> cmds = CombinePrimitiveResolverDialog.launchIfNecessary(
                    tagColl, prims, Collections.singleton(platformNode));
            Main.main.undoRedo.add(new SequenceCommand("merging", cmds));
        } catch (UserCancelException ex) {
            Main.trace(ex);
        } finally {
            Main.main.undoRedo.add(new DeleteCommand(dummy1));
            Main.main.undoRedo.add(new DeleteCommand(dummy2));
            Main.main.undoRedo.add(new DeleteCommand(dummy3));
        }
    }

    public void populateMap(OsmPrimitive prim) {
        List<String> unInterestingTags = new ArrayList<>();
        unInterestingTags.add("public_transport");
        unInterestingTags.add("highway");
        unInterestingTags.add("source");

        for (Entry<String, String> tag: prim.getKeys().entrySet()) {
            if (unInterestingTags.contains(tag.getKey())) {
                continue;
            }
            if (dummy1.get(tag.getKey()) == null) {
                dummy1.put(tag.getKey(), tag.getValue());
            } else if (dummy2.get(tag.getKey()) == null) {
                dummy2.put(tag.getKey(), tag.getValue());
            } else if (dummy3.get(tag.getKey()) == null) {
                dummy3.put(tag.getKey(), tag.getValue());
            }
        }
    }

    @Override
    protected void updateEnabledState(
            Collection<? extends OsmPrimitive> selection) {
        setEnabled(false);

        if (selection.size() > 1) {
            setEnabled(true);
        }
    }
}

