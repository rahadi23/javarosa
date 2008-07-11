package org.javarosa.demo.shell;

import java.util.Hashtable;

import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;

import org.javarosa.communication.http.HttpTransportMethod;
import org.javarosa.communication.http.HttpTransportProperties;
import org.javarosa.core.Context;
import org.javarosa.core.JavaRosaServiceProvider;
import org.javarosa.core.api.Constants;
import org.javarosa.core.api.IActivity;
import org.javarosa.core.api.IShell;
import org.javarosa.core.model.FormData;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.storage.FormDataRMSUtility;
import org.javarosa.core.model.storage.FormDefRMSUtility;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.core.util.WorkflowStack;
import org.javarosa.demo.module.SplashScreenModule;
import org.javarosa.demo.properties.DemoAppProperties;
import org.javarosa.formmanager.activity.FormListActivity;
import org.javarosa.formmanager.activity.FormTransportActivity;
import org.javarosa.formmanager.activity.ModelListActivity;
import org.javarosa.formmanager.utility.TransportContext;
import org.javarosa.formmanager.view.Commands;
import org.javarosa.services.properties.activity.PropertyModule;
import org.javarosa.xform.util.XFormUtils;

/**
 * This is the shell for the JavaRosa demo that handles switching all of the views
 * @author Brian DeRenzi
 *
 */
public class JavaRosaDemoShell implements IShell {
	// List of views that are used by this shell
	MIDlet runningAssembly;
	FormListActivity formModule = null;
	SplashScreenModule splashScreen = null;
	FormTransportActivity formTransport = null;
	ModelListActivity modelModule = null;
	PropertyModule propertyModule = null;
	
	WorkflowStack stack;
	
	Context context;
	
	IActivity currentModule;

	public JavaRosaDemoShell() {
		stack = new WorkflowStack(); 
		context = new Context();
	}

	public void exitShell() {
		runningAssembly.notifyDestroyed();
	}
	
	public void run() {
		init();
		System.out.println("done init");
		this.splashScreen = new SplashScreenModule(this, "/splash.gif");
		System.out.println("done splash init");
		this.formModule = new FormListActivity(this,"Forms List");
		System.out.println("Done formlist init");
		this.formTransport = new FormTransportActivity(this);
		this.modelModule = new ModelListActivity(this);
		
		this.propertyModule = new PropertyModule(this);
		
		currentModule = splashScreen;
		this.splashScreen.start(context);
		System.out.println("Done with splashscreen start");
	//	switchView(ViewTypes.FORM_LIST);
	}
	
	private void init() {
		
		JavaRosaServiceProvider.instance().getPropertyManager().addRules(new JavaRosaPropertyRules());
		JavaRosaServiceProvider.instance().getPropertyManager().addRules(new DemoAppProperties());
		JavaRosaServiceProvider.instance().getPropertyManager().addRules(new HttpTransportProperties());
		
		JavaRosaServiceProvider.instance().getTransportManager().registerTransportMethod(new HttpTransportMethod());
		
		FormDataRMSUtility formData = new FormDataRMSUtility(FormDataRMSUtility.getUtilityName());
		FormDefRMSUtility formDef = new FormDefRMSUtility(FormDefRMSUtility.getUtilityName());

		System.out.println("Loading Forms");
		// For now let's add the dummy form.
		if (formDef.getNumberOfRecords() == 0) {
			formDef.writeToRMS(XFormUtils
					.getFormFromResource("/hmis-a_draft.xhtml"));
			formDef.writeToRMS(XFormUtils
					.getFormFromResource("/hmis-b_draft.xhtml"));
			formDef.writeToRMS(XFormUtils
					.getFormFromResource("/shortform.xhtml"));
		}
		System.out.println("Done Loading Forms");
		JavaRosaServiceProvider.instance().getStorageManager().getRMSStorageProvider()
				.registerRMSUtility(formData);
		JavaRosaServiceProvider.instance().getStorageManager().getRMSStorageProvider()
				.registerRMSUtility(formDef);
		System.out.println("Done registering");
	}
	
	private void workflow(IActivity lastModule, String returnCode, Hashtable returnVals) {
		//TODO: parse any returnvals into context
		if(stack.size() != 0) {
			IActivity activity = stack.pop();
			this.currentModule = activity;
			activity.resume(context);
		}
		else {
			// TODO Auto-generated method stub
			if (lastModule == this.splashScreen) {
				currentModule = formModule;
				this.formModule.start(context);
			}
			if (lastModule == this.modelModule) {
				if (returnCode == Constants.ACTIVITY_NEEDS_RESOLUTION) {
					Object returnVal = returnVals.get(ModelListActivity.returnKey);
					if (returnVal == ModelListActivity.CMD_MSGS) {
						// Go to the FormTransport Module look at messages.
						TransportContext msgContext = new TransportContext(
								context);
						msgContext.setRequestedTask(TransportContext.MESSAGE_VIEW);
						currentModule = formTransport;
						formTransport.start(msgContext);
					}
				}
				if (returnCode == Constants.ACTIVITY_COMPLETE) {
					// A Model was selected for some purpose
					Object returnVal = returnVals
							.get(ModelListActivity.returnKey);
					if (returnVal == ModelListActivity.CMD_EDIT) {
						// Load the Form Entry Module, and feed it the form data
						FormDef form = (FormDef) returnVals.get("form");
						FormData data = (FormData) returnVals.get("data");
					}
					if (returnVal == ModelListActivity.CMD_SEND) {
						FormData data = (FormData) returnVals.get("data");
						formTransport.setData(data);
						TransportContext msgContext = new TransportContext(
								context);
						msgContext.setRequestedTask(TransportContext.SEND_DATA);
						currentModule = formTransport;
						formTransport.start(msgContext);
					}
				}
			}
			if (lastModule == this.formTransport) {
				if (returnCode == Constants.ACTIVITY_NEEDS_RESOLUTION) {
					String returnVal = (String)returnVals.get(FormTransportActivity.RETURN_KEY);
					if(returnVal == FormTransportActivity.VIEW_MODELS) {
						currentModule = this.modelModule;
						this.modelModule.start(context);
					}
				}
				if (returnCode == Constants.ACTIVITY_COMPLETE) {
					
				}
			}
			if (lastModule == this.formModule) {
				if (returnCode == Constants.ACTIVITY_NEEDS_RESOLUTION) {
					String returnVal = (String)returnVals.get("command");
					if(returnVal == Commands.CMD_VIEW_DATA) {
						currentModule = this.modelModule;
						this.modelModule.start(context);
					}
					if(returnVal == Commands.CMD_SETTINGS) {
						currentModule = this.propertyModule;
						this.propertyModule.start(context);
					}
				}
				if (returnCode == Constants.ACTIVITY_COMPLETE) {
					
				}
			}
		}
		if(currentModule == lastModule) {
			//We didn't launch anything. Go to default
			currentModule = formModule;
			formModule.start(context);
		}
	}

	/* (non-Javadoc)
	 * @see org.javarosa.shell.IShell#moduleCompeleted(org.javarosa.module.IModule)
	 */
	public void returnFromModule(IActivity activity, String returnCode, Hashtable returnVals) {
		activity.halt();
		System.out.println("Module: " + activity + " returned with code " + returnCode);
		workflow(activity, returnCode, returnVals);
		if(returnCode == Constants.ACTIVITY_SUSPEND || returnCode == Constants.ACTIVITY_NEEDS_RESOLUTION) {
			stack.push(activity);
		}
	}

	public void setDisplay(IActivity callingModule, Displayable display) {
		if(callingModule == currentModule) {
			JavaRosaServiceProvider.instance().getDisplay().setCurrent(display);
		}
		else {
			System.out.println("Module: " + callingModule + " attempted, but failed, to set the display");
		}
	}
	
	public void setRunningAssembly(MIDlet assembly) {
		this.runningAssembly = assembly;
	}
}
