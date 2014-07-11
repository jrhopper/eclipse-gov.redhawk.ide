package gov.redhawk.ide.tests.ui;

import gov.redhawk.ide.debug.LocalSca;
import gov.redhawk.ide.debug.LocalScaDeviceManager;
import gov.redhawk.ide.debug.ScaDebugPlugin;
import gov.redhawk.ide.tests.ui.stubs.AnalogDevice;
import gov.redhawk.model.sca.RefreshDepth;
import gov.redhawk.model.sca.ScaDevice;
import gov.redhawk.sca.ui.views.ScaExplorer;
import gov.redhawk.sca.util.OrbSession;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import CF.ExecutableDevice;
import CF.ExecutableDeviceHelper;
import CF.ExecutableDevicePOATie;
import CF.InvalidObjectReference;

@RunWith(SWTBotJunit4ClassRunner.class)
public class FEIScaExplorerTest {
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				final IIntroManager introManager = PlatformUI.getWorkbench().getIntroManager();
				if (introManager != null) {
					IIntroPart part = introManager.getIntro();
					if (part != null) {
						introManager.closeIntro(part);
					}
				}
			}
		});
		
		session = OrbSession.createSession();
		session.getPOA();
	}

	private SWTWorkbenchBot bot;
	private ExecutableDevice ref;
	private SWTBotView explorerView;
	private SWTBot viewBot;
	private SWTBotTree explorerTree;
	private LocalSca localSca;
	private LocalScaDeviceManager devMgr;
	private static OrbSession session;
	
	@Before
	public void setup() throws Exception {
		bot = new SWTWorkbenchBot();
		SWTBotPerspective perspective = bot.perspectiveById("gov.redhawk.ide.ui.perspectives.sca");
		perspective.activate();
		bot.resetActivePerspective();
		
		bot = new SWTWorkbenchBot();
		
		explorerView = bot.viewById(ScaExplorer.VIEW_ID);
		explorerView.show();
		viewBot = explorerView.bot();
		explorerTree = viewBot.tree();
		
		localSca = ScaDebugPlugin.getInstance().getLocalSca(null);
		devMgr = localSca.getSandboxDeviceManager();
		
		AnalogDevice stubDevice = new AnalogDevice();
		ref =  ExecutableDeviceHelper.narrow(session.getPOA().servant_to_reference(new ExecutableDevicePOATie(stubDevice)));
		devMgr.registerDevice(ref);
		
		devMgr.fetchDevices(null);
		ScaDevice< ? > device = devMgr.getDevice("analogDevice");
		device.refresh(null, RefreshDepth.SELF);
		
		viewBot.sleep(500);
	}
	
	@AfterClass
	public static void classCleanup() {
		if (session != null) {
			session.dispose();
			session = null;
		}
	}
	
	@After
	public void cleanUp() {
		if (ref != null) {
			try {
				devMgr.unregisterDevice(ref);
			} catch (InvalidObjectReference e) {
				// PASS
			}
		}
	}
	
	@Test
	public void test_IDE_797() throws Exception {
		explorerTree.expandNode("Sandbox", "Device Manager", "analogDevice", "FrontEnd Tuners");
	}

}
