/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.ui.commandrunner;


import com.google.common.base.Strings;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.safehaus.subutai.api.agentmanager.AgentManager;
import org.safehaus.subutai.api.commandrunner.*;
import org.safehaus.subutai.server.ui.component.AgentTree;
import org.safehaus.subutai.shared.protocol.Agent;
import org.safehaus.subutai.shared.protocol.Disposable;
import org.safehaus.subutai.shared.protocol.Response;
import org.safehaus.subutai.shared.protocol.Util;
import org.safehaus.subutai.shared.protocol.enums.RequestType;
import org.safehaus.subutai.shared.protocol.enums.ResponseType;
import org.safehaus.subutai.shared.protocol.settings.Common;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Command Runner UI - Terminal
 */
public class TerminalForm extends CustomComponent implements Disposable {

	final CommandRunner commandRunner;
	final AgentManager agentManager;
	private volatile int taskCount = 0;
	private ExecutorService executor;
	//
	private HorizontalSplitPanel horizontalSplit;
	private AgentTree agentTree;
	private TextArea commandOutputTxtArea;
	//
	private TextField programTxtFld, workDirTxtFld, timeoutTxtFld;
	private ComboBox requestTypeCombo;
	private Button clearBtn, sendBtn;
	private Label indicator;


	public TerminalForm(final CommandRunner commandRunner, final AgentManager agentManager) {
		this.commandRunner = commandRunner;
		this.agentManager = agentManager;

		executor = Executors.newCachedThreadPool();

		setSizeFull();

		horizontalSplit = new HorizontalSplitPanel();
		horizontalSplit.setSplitPosition(200, Unit.PIXELS);
		horizontalSplit.setSizeFull();

		agentTree = new AgentTree(agentManager);
		horizontalSplit.setFirstComponent(agentTree);

		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.setSizeFull();
		verticalLayout.setMargin(true);
		verticalLayout.setSpacing(true);

		initOutputTextArea();
		verticalLayout.addComponent(commandOutputTxtArea);

		HorizontalLayout controls = new HorizontalLayout();
		controls.setSpacing(true);
		controls.setMargin(true);

		initProgram(controls);
		initCommand(controls);
		initTimeout(controls);
		initRequestType(controls);
		initClearButton(controls);
		initSendButton(controls);
		initIndicator(controls);

		verticalLayout.addComponent(controls);

		horizontalSplit.setSecondComponent(verticalLayout);
		setCompositionRoot(horizontalSplit);
	}

	private void initOutputTextArea() {
		commandOutputTxtArea = new TextArea("Commands output");
		commandOutputTxtArea.setSizeFull();
		commandOutputTxtArea.setImmediate(true);
		commandOutputTxtArea.setWordwrap(false);
	}

	private void initIndicator(HorizontalLayout controls) {
		indicator = new Label();
		indicator.setIcon(new ThemeResource("img/spinner.gif"));
		indicator.setContentMode(ContentMode.HTML);
		indicator.setHeight(11, Unit.PIXELS);
		indicator.setWidth(50, Unit.PIXELS);
		indicator.setVisible(false);
		controls.addComponent(indicator);
	}

	private void initSendButton(HorizontalLayout controls) {
		sendBtn = new Button("Send");
		controls.addComponent(sendBtn);
		sendBtn.addStyleName("default");

		sendBtn.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(Button.ClickEvent clickEvent) {
				Set<Agent> agents = checkAgents();
				if (agents != null && validateInputs()) {
					RequestBuilder requestBuilder = new RequestBuilder(programTxtFld.getValue());

					if (requestTypeCombo.getValue() == RequestType.TERMINATE_REQUEST) {
						requestBuilder.withPid(Integer.valueOf(programTxtFld.getValue()));
						requestBuilder.withType(RequestType.TERMINATE_REQUEST);
					} else if (requestTypeCombo.getValue() == RequestType.PS_REQUEST) {
						requestBuilder.withType(RequestType.PS_REQUEST);
					}

					requestBuilder.withTimeout(Integer.valueOf(timeoutTxtFld.getValue()));
					requestBuilder.withCwd(workDirTxtFld.getValue());

					getUI().setPollInterval(Common.REFRESH_UI_SEC * 1000);
					createCommand(requestBuilder, agents);
				}
			}
		});
	}

	private void initClearButton(HorizontalLayout controls) {
		clearBtn = new Button("Clear");
		clearBtn.addStyleName("default");
		controls.addComponent(clearBtn);

		clearBtn.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(Button.ClickEvent clickEvent) {
				commandOutputTxtArea.setValue("");
			}
		});
	}

	private void initRequestType(HorizontalLayout controls) {
		Label requestTypeLabel = new Label("Req Type");
		controls.addComponent(requestTypeLabel);
		requestTypeCombo = new ComboBox(null,
				Arrays.asList(RequestType.EXECUTE_REQUEST, RequestType.TERMINATE_REQUEST, RequestType.PS_REQUEST));
		requestTypeCombo.setImmediate(true);
		requestTypeCombo.setTextInputAllowed(false);
		requestTypeCombo.setNullSelectionAllowed(false);
		requestTypeCombo.setValue(RequestType.EXECUTE_REQUEST);
		requestTypeCombo.setWidth(200, Unit.PIXELS);
		controls.addComponent(requestTypeCombo);
	}

	private void initTimeout(HorizontalLayout controls) {
		Label timeoutLbl = new Label("Timeout");
		timeoutTxtFld = new TextField();
		timeoutTxtFld.setValue("30");
		timeoutTxtFld.setWidth(30, Unit.PIXELS);
		controls.addComponent(timeoutLbl);
		controls.addComponent(timeoutTxtFld);
	}

	private void initCommand(HorizontalLayout controls) {
		Label workDirLbl = new Label("Cwd");
		workDirTxtFld = new TextField();
		workDirTxtFld.setValue("/");
		controls.addComponent(workDirLbl);
		controls.addComponent(workDirTxtFld);
	}

	private void initProgram(HorizontalLayout controls) {
		Label programLbl = new Label("Program");
		programTxtFld = new TextField();
		programTxtFld.setValue("pwd");
		programTxtFld.setWidth(300, Unit.PIXELS);
		controls.addComponent(programLbl);
		controls.addComponent(programTxtFld);

		programTxtFld.addShortcutListener(new ShortcutListener("Shortcut Name", ShortcutAction.KeyCode.ENTER, null) {
			@Override
			public void handleAction(Object sender, Object target) {
				sendBtn.click();
			}
		});
	}

	private void show(String notification) {
		Notification.show(notification);
	}

	private void addOutput(String output) {
		if (!Strings.isNullOrEmpty(output)) {
			commandOutputTxtArea.setValue(String.format("%s%s", commandOutputTxtArea.getValue(), output));
			commandOutputTxtArea.setCursorPosition(commandOutputTxtArea.getValue().toString().length() - 1);
		}
	}

	public void dispose() {
		agentTree.dispose();
		executor.shutdown();
	}

	private Set<Agent> checkAgents() {
		Set<Agent> agents = agentTree.getSelectedAgents();
		if (agents.isEmpty()) {
			agents = null;
			show("Please, select nodes");
		}

		return agents;
	}

	private boolean validateInputs() {

		if (Strings.isNullOrEmpty(programTxtFld.getValue())) {
			show("Please, enter command");
			return false;
		}

		/*if (!Util.isNumeric(programTxtFld.getValue())
				|| Integer.valueOf(programTxtFld.getValue()) <= 0) {
			show("Please, enter numeric PID greater than 0 to kill");
			return false;
		}*/

		if (Strings.isNullOrEmpty(timeoutTxtFld.getValue()) || !Util.isNumeric(timeoutTxtFld.getValue())) {
			show("Please, enter integer timeout value");
			return false;
		} else {
			int timeout = Integer.valueOf(timeoutTxtFld.getValue().toString());
			if (timeout <= 0 || timeout > Common.MAX_COMMAND_TIMEOUT_SEC) {
				show("Please, enter timeout value between 0 and " + Common.MAX_COMMAND_TIMEOUT_SEC);
			}
		}

		if (Strings.isNullOrEmpty(workDirTxtFld.getValue())) {
			show("Please, enter working directory");
			return false;
		}

		return true;
	}

	private void createCommand(RequestBuilder requestBuilder, Set<Agent> agents) {
		final Command command = commandRunner.createCommand(requestBuilder, agents);
		indicator.setVisible(true);
		taskCount++;
		executor.execute(new Runnable() {

			public void run() {
				commandRunner.runCommand(command, new CommandCallback() {

					@Override
					public void onResponse(Response response, AgentResult agentResult, Command command) {
						Agent agent = agentManager.getAgentByUUID(response.getUuid());
						String host = agent == null ? String.format("Offline[%s]", response.getUuid()) :
								agent.getHostname();
						StringBuilder out =
								new StringBuilder(host).append(" [").append(response.getPid())
										.append("]").append(":\n");
						if (!Strings.isNullOrEmpty(response.getStdOut())) {
							out.append(response.getStdOut()).append("\n");
						}
						if (!Strings.isNullOrEmpty(response.getStdErr())) {
							out.append(response.getStdErr()).append("\n");
						}
						if (response.isFinal()) {
							if (response.getType() == ResponseType.EXECUTE_RESPONSE_DONE) {
								out.append("Exit code: ").append(response.getExitCode())
										.append("\n\n");
							} else {
								out.append(response.getType()).append("\n\n");
							}
						}
						addOutput(out.toString());
						getUI().setPollInterval(Common.REFRESH_UI_SEC * 60000);
					}
				});

				taskCount--;
				if (taskCount == 0) {
					indicator.setVisible(false);
				}
			}
		});
	}
}
