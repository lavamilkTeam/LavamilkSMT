package org.openpnp.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import org.openpnp.Translations;
import org.openpnp.smt.McuUpdateClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** A read-only update preflight. Real maintenance authorization belongs to Python. */
public final class McuUpdateAction extends AbstractAction {
    private final Component parent;

    public McuUpdateAction(Component parent) {
        super(Translations.getString("McuUpdate.Action"));
        this.parent = parent;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        setEnabled(false);
        putValue(NAME, Translations.getString("McuUpdate.Checking"));
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                String host = System.getProperty("smt.controller.host", "127.0.0.1");
                int port = Integer.parseInt(System.getProperty("smt.controller.port", "8765"));
                return new McuUpdateClient(host, port).check();
            }

            @Override
            protected void done() {
                try {
                    JsonObject result = get();
                    StringBuilder text = new StringBuilder(Translations.getString("McuUpdate.ReadOnly"));
                    if (result.has("release") && result.get("release").isJsonObject()) {
                        text.append("\n\n").append(Translations.getString("McuUpdate.Release"))
                                .append(" ")
                                .append(result.getAsJsonObject("release").get("version").getAsString());
                    }
                    if (result.has("blockers") && result.get("blockers").isJsonArray()) {
                        for (JsonElement reason : result.getAsJsonArray("blockers")) {
                            text.append("\n\n• ").append(reason.getAsString());
                        }
                    }
                    show(text.toString(), JOptionPane.INFORMATION_MESSAGE);
                }
                catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    show(Translations.getString("McuUpdate.Unavailable") + "\n\n" + cause.getMessage(),
                            JOptionPane.ERROR_MESSAGE);
                }
                finally {
                    putValue(NAME, Translations.getString("McuUpdate.Action"));
                    setEnabled(true);
                }
            }
        }.execute();
    }

    private void show(String message, int kind) {
        JTextArea text = new JTextArea(message, 14, 64);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setCaretPosition(0);
        JOptionPane.showMessageDialog(parent, new JScrollPane(text),
                Translations.getString("McuUpdate.Title"), kind);
    }
}
