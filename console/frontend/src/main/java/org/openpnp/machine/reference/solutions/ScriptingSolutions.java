package org.openpnp.machine.reference.solutions;

import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Configuration;

import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;

/**
 * This helper class implements the Issues & Solutions for the Scripting class.
 *
 */
public class ScriptingSolutions implements Solutions.Subject {
    private ReferenceMachine machine;

    public ScriptingSolutions setMachine(ReferenceMachine machine) {
        this.machine = machine;
        return this;
    }

    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.isTargeting(Milestone.Advanced)) {
            if (Configuration.get()
                             .getMachine()
                             .isPoolScriptingEngines() == false) {
                solutions.add(new Solutions.Issue(machine,
                        org.openpnp.Translations.message("Local.fb482fa581331aa0"),
                        org.openpnp.Translations.message("Local.c0ddafc5a23e4286"), Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Scripting#script-engine-pooling") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        ((ReferenceMachine) Configuration.get()
                                                         .getMachine()).setPoolScriptingEngines(
                                                                 state == State.Solved ? true
                                                                         : false);
                        super.setState(state);
                    }

                    @Override
                    public String getExtendedDescription() {
                        return org.openpnp.Translations.format("Local.84bce9f5ce0f49fb");
                    }
                });
            }
        }
    }
}
