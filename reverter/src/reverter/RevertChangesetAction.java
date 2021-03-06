// License: GPL. For details, see LICENSE file.
package reverter;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.Shortcut;

import reverter.ChangesetReverter.RevertType;

@SuppressWarnings("serial")
public class RevertChangesetAction extends JosmAction {

    public RevertChangesetAction() {
        super(tr("Revert changeset"), "revert-changeset", tr("Revert changeset"),
            Shortcut.registerShortcut("tool:revert",
                tr("Tool: {0}", tr("Revert changeset")),
                KeyEvent.VK_T, Shortcut.CTRL_SHIFT),
                true);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        final ChangesetIdQuery dlg = new ChangesetIdQuery();
        if (dlg.showDialog().getValue() != 1) return;
        final Collection<Integer> changesetIds = dlg.getIdsInReverseOrder();
        final RevertType revertType = dlg.getRevertType();
        if (revertType == null) return;

        boolean newLayer = dlg.isNewLayerRequired();
        final boolean autoConfirmDownload = newLayer || changesetIds.size() > 1;
        for (Integer changesetId : changesetIds) {
            Main.worker.submit(new RevertChangesetTask(changesetId, revertType, autoConfirmDownload, newLayer));
            newLayer = false; // reuse layer for subsequent reverts
        }
    }
}
