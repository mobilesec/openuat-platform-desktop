/* Copyright Rene Mayrhofer
 * File created 2006-05-16
 * Initial public release 2007-03-29
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */
package org.openuat.apps;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openuat.authentication.accelerometer.ShakeWellBeforeUseParameters;
import org.openuat.authentication.accelerometer.ShakeWellBeforeUseProtocol1;
import org.openuat.authentication.accelerometer.ShakeWellBeforeUseProtocol2;
import org.openuat.channel.main.RemoteConnection;
import org.openuat.channel.main.ip.RemoteTCPConnection;
import org.openuat.channel.main.ip.TCPPortServer;
import org.openuat.sensors.AsciiLineReaderBase;
import org.openuat.sensors.ParallelPortPWMReader;
import org.openuat.sensors.SamplesSink;
import org.openuat.sensors.TimeSeriesAggregator;
import org.openuat.sensors.WiTiltRawReader;

/** This is a simple demonstrator for the shaking authentication. It
 * shows both protocol variants with a simple GUI.
 * @author Rene Mayrhofer
 * @version 1.0
 */
public class ShakingSinglePCDemonstrator {
	/** Our logger. */
	private static Logger logger = LoggerFactory.getLogger(ShakingSinglePCDemonstrator.class.getName());
	
	/** The displayed text when a device is active. */
	private static String DEVICE_STATE_ACTIVE = "active";
	/** The displayed text when a device is quiescent. */
	private static String DEVICE_STATE_QUIESCENT = "quiescent";

	private Shell sShell = null;  //  @jve:decl-index=0:visual-constraint="10,10"
	private Composite coherenceField = null;
	private Composite matchingField = null;
	private Label coherence = null;
	private Label matching = null;
	private Label coherenceValue = null;
	private Label matchingValue = null;
	private Label device1 = null;
	private Label device2 = null;
	private Label[] deviceStates = null;
	
	private Protocol1Hooks prot1_a = null;
	private Protocol1Hooks prot1_b = null;
	private Protocol2Hooks prot2_a = null;
	private Protocol2Hooks prot2_b = null;
	
	private StateListener devState1 = null;
	private StateListener devState2 = null;
	
	private AsciiLineReaderBase reader1 = null;
	private AsciiLineReaderBase reader2 = null;

	/**
	 * This method initializes composite	
	 *
	 */
	private void createComposite() {
		coherenceField = new Composite(sShell, SWT.NONE);
		coherenceField.setBackground(new Color(Display.getDefault(), 227, 227, 255));
		coherenceField.setBounds(new org.eclipse.swt.graphics.Rectangle(18,120,370,300));
		coherence = new Label(coherenceField, SWT.NONE);
		coherence.setBounds(new org.eclipse.swt.graphics.Rectangle(17,15,334,36));
		coherence.setFont(new Font(Display.getDefault(), "Sans", 18, SWT.NORMAL));
		coherence.setText("Method 1:");
		coherenceValue = new Label(coherenceField, SWT.NONE);
		coherenceValue.setBounds(new org.eclipse.swt.graphics.Rectangle(17,55,334,36));
		coherenceValue.setFont(new Font(Display.getDefault(), "Sans", 24, SWT.NORMAL));
		coherenceValue.setText("0");
	}

	/**
	 * This method initializes composite1	
	 *
	 */
	private void createComposite1() {
		matchingField = new Composite(sShell, SWT.NONE);
		matchingField.setBackground(new Color(Display.getDefault(), 227, 227, 255));
		matchingField.setBounds(new org.eclipse.swt.graphics.Rectangle(394,120,370,300));
		matching = new Label(matchingField, SWT.NONE);
		matching.setBounds(new org.eclipse.swt.graphics.Rectangle(17,15,334,36));
		matching.setFont(new Font(Display.getDefault(), "Sans", 18, SWT.NORMAL));
		matching.setText("Method 2:");
		matchingValue = new Label(matchingField, SWT.NONE);
		matchingValue.setBounds(new org.eclipse.swt.graphics.Rectangle(17,55,334,36));
		matchingValue.setFont(new Font(Display.getDefault(), "Sans", 24, SWT.NORMAL));
		matchingValue.setText("0");
	}

	/**
	 * This method initializes sShell
	 */
	private void createSShell() {
		sShell = new Shell();
		sShell.setText("Shake well before use");
		
		device1 = new Label(sShell, SWT.NONE);
		device1.setBounds(new org.eclipse.swt.graphics.Rectangle(18,10,200,30));
		device1.setFont(new Font(Display.getDefault(), "Sans", 18, SWT.NORMAL));
		device1.setAlignment(SWT.LEFT);
		device1.setText("Device 1:");
		device2 = new Label(sShell, SWT.NONE);
		device2.setBounds(new org.eclipse.swt.graphics.Rectangle(18,40,200,30));
		device2.setFont(new Font(Display.getDefault(), "Sans", 18, SWT.NORMAL));
		device2.setAlignment(SWT.LEFT);
		device2.setText("Device 2:");

		deviceStates = new Label[2];
		deviceStates[0] = new Label(sShell, SWT.NONE);
		deviceStates[0].setBounds(new org.eclipse.swt.graphics.Rectangle(230,10,300,30));
		deviceStates[0].setFont(new Font(Display.getDefault(), "Sans", 18, SWT.NORMAL));
		deviceStates[0].setAlignment(SWT.LEFT);
		deviceStates[0].setText(DEVICE_STATE_QUIESCENT);
		deviceStates[1] = new Label(sShell, SWT.NONE);
		deviceStates[1].setBounds(new org.eclipse.swt.graphics.Rectangle(230,40,300,30));
		deviceStates[1].setFont(new Font(Display.getDefault(), "Sans", 18, SWT.NORMAL));
		deviceStates[1].setAlignment(SWT.LEFT);
		deviceStates[1].setText(DEVICE_STATE_QUIESCENT);
		
		createComposite();
		createComposite1();
		sShell.setSize(new org.eclipse.swt.graphics.Point(786,515));
	}
	
	/** The only constructor for the shaking demonstrator. 
	 * @param device1 If deviceType is set to 1, this specifies the log file (or pipe) to
	 *                read the pulse-width parallel port data. A special case is a syntax
	 *                "port:<port number>" to open an TCP port and listen for incoming log
	 *                lines on that port. If deviceType is set to 2 or 3,
	 *                this specifies the name of the first serial port or the first Bluetooth
	 *                MAC address, respectively, to read from the WiTilt sensor.
	 * @param device2 If deviceType is set to 2, this specifies the name of the second 
	 *                serial port or the second Bluetooth MAC address to read from the 
	 *                WiTilt sensor. If deviceType is set to 1, it is simply ignored.
	 * @param deviceType The sensor device type to use. If set to 1, pulse-width signals
	 *                   will be sampled from the parallel port. If set to 2, WiTilt devices
	 *                   will be used with a (virtual) serial port, if set to 3, WiTilt devices
	 *                   will be used via JSR82 RFCOMM channels.
	 * @throws IOException
	 */
	public ShakingSinglePCDemonstrator(String device1, String device2, int deviceType) throws IOException {
		/* 1: construct the central sensor reader object and the two segment aggregators */
		
		/* First of all, open the display so that there's feedback and so that the events can write
		 * to an open display.
		 */
		createSShell();
		sShell.open();
		
		// need two aggregators, because the first protocols works with 5s-slices
		final TimeSeriesAggregator aggr1_a = new TimeSeriesAggregator(3, ShakeWellBeforeUseParameters.activityDetectionWindowSize, ShakeWellBeforeUseParameters.coherenceSegmentSize, ShakeWellBeforeUseParameters.coherenceSegmentSize);
		final TimeSeriesAggregator aggr1_b = new TimeSeriesAggregator(3, ShakeWellBeforeUseParameters.activityDetectionWindowSize, ShakeWellBeforeUseParameters.coherenceSegmentSize, ShakeWellBeforeUseParameters.coherenceSegmentSize);
		// these can use segments of arbitrary length
		final TimeSeriesAggregator aggr2_a = new TimeSeriesAggregator(3, ShakeWellBeforeUseParameters.activityDetectionWindowSize, ShakeWellBeforeUseParameters.activityMinimumSegmentSize, -1);
		final TimeSeriesAggregator aggr2_b = new TimeSeriesAggregator(3, ShakeWellBeforeUseParameters.activityDetectionWindowSize, ShakeWellBeforeUseParameters.activityMinimumSegmentSize, -1);
		aggr1_a.setActiveVarianceThreshold(ShakeWellBeforeUseParameters.activityVarianceThreshold);
		aggr1_b.setActiveVarianceThreshold(ShakeWellBeforeUseParameters.activityVarianceThreshold);
		aggr2_a.setActiveVarianceThreshold(ShakeWellBeforeUseParameters.activityVarianceThreshold);
		aggr2_b.setActiveVarianceThreshold(ShakeWellBeforeUseParameters.activityVarianceThreshold);
		// including our listeners for the device status
		devState1 = new StateListener(0);
		devState2 = new StateListener(1);
		// this uses the more "continuous" second set of aggregators
		aggr2_a.addNextStageSamplesSink(devState1);
		aggr2_b.addNextStageSamplesSink(devState2);

		/* 2: construct the two prototol instances: two different variants, each with two sides */
		prot1_a = new Protocol1Hooks();
		prot1_b = new Protocol1Hooks();
		// TODO: move this threshold into ShakeWellBeforeUseProtocol2
		prot2_a = new Protocol2Hooks(5, 56789, 56798, "A");
		prot2_b = new Protocol2Hooks(5, 56798, 56789, "B");
		
		/* 3: register the protocols with the respective sides */
		aggr1_a.addNextStageSegmentsSink(prot1_a);
		aggr1_b.addNextStageSegmentsSink(prot1_b);
		aggr2_a.addNextStageSamplesSink(prot2_a);
		aggr2_b.addNextStageSamplesSink(prot2_b);
		
		/* 4: authenticate for protocol variant 1 (variant 2 doesn't need this step) */
		prot1_a.setContinuousChecking(true);
		prot1_b.setContinuousChecking(true);
		prot1_a.startListening();
		prot1_b.startAuthentication(new RemoteTCPConnection(
				new Socket("localhost", ShakeWellBeforeUseProtocol1.TcpPort)), 
				ShakeWellBeforeUseProtocol1.KeyAgreementProtocolTimeout, null);
		
		if (deviceType == 1) {
			if (! device1.startsWith("port:")) {
				// just read from the file
				reader1 = new ParallelPortPWMReader(device1, ShakeWellBeforeUseParameters.samplerate);
				aggr1_a.setParameters(reader1.getParameters());
				aggr1_b.setParameters(reader1.getParameters());
				aggr2_a.setParameters(reader1.getParameters());
				aggr2_b.setParameters(reader1.getParameters());

				reader1.addSink(new int[] {0, 1, 2}, aggr1_a.getInitialSinks());
				reader1.addSink(new int[] {4, 5, 6}, aggr1_b.getInitialSinks());
				reader1.addSink(new int[] {0, 1, 2}, aggr2_a.getInitialSinks());
				reader1.addSink(new int[] {4, 5, 6}, aggr2_b.getInitialSinks());
				reader1.start();
			}
			else {
				// open an UDP socket and read from there
				int port = Integer.parseInt(device1.substring(5));
				logger.info("Creating TCP listening socket on port " + port);
				final ServerSocket serv = new ServerSocket(port);
				new Thread(new Runnable() { 
					public void run() {
						try {
							while (true) {
								logger.info("Waiting for TCP client to connect");
								aggr1_a.reset();
								aggr1_b.reset();
								aggr2_a.reset();
								aggr2_b.reset();
								Socket sock = serv.accept();
								logger.info("Client " + sock.getRemoteSocketAddress() + " connected");
								try {
									reader1 = new ParallelPortPWMReader(sock.getInputStream(), ShakeWellBeforeUseParameters.samplerate);
									aggr1_a.setParameters(reader1.getParameters());
									aggr1_b.setParameters(reader1.getParameters());
									aggr2_a.setParameters(reader1.getParameters());
									aggr2_b.setParameters(reader1.getParameters());
									reader1.addSink(new int[] {0, 1, 2}, aggr1_a.getInitialSinks());
									reader1.addSink(new int[] {4, 5, 6}, aggr1_b.getInitialSinks());
									reader1.addSink(new int[] {0, 1, 2}, aggr2_a.getInitialSinks());
									reader1.addSink(new int[] {4, 5, 6}, aggr2_b.getInitialSinks());
									reader1.simulateSampling();
								}
								catch (IOException e) {
									logger.warn("Could not read from remote host " + sock.getRemoteSocketAddress() + 
										", most probably client terminated connection. Disconnecting.");
								}
							}
						}	
						catch (IOException e) {
							logger.error("Could not accept connection from socket: " + e);
						}
					}
				}).start();
			}

		}
		else if (deviceType == 2 || deviceType == 3) {
			reader1 = new WiTiltRawReader();
			reader2 = new WiTiltRawReader();
			if (deviceType == 2) {
				((WiTiltRawReader) reader1).openSerial(device1, false);
				((WiTiltRawReader) reader2).openSerial(device2, false);
			}
			else {
				((WiTiltRawReader) reader1).openBluetooth(device1, false);
				((WiTiltRawReader) reader2).openBluetooth(device2, false);
			}
			aggr1_a.setParameters(reader1.getParameters());
			aggr1_b.setParameters(reader1.getParameters());
			aggr2_a.setParameters(reader1.getParameters());
			aggr2_b.setParameters(reader1.getParameters());

			reader1.addSink(new int[] {0, 1, 2}, aggr1_a.getInitialSinks());
			reader2.addSink(new int[] {0, 1, 2}, aggr1_b.getInitialSinks());
			reader1.addSink(new int[] {0, 1, 2}, aggr2_a.getInitialSinks());
			reader2.addSink(new int[] {0, 1, 2}, aggr2_b.getInitialSinks());
			reader1.start();
			reader2.start();
		}
		else
			throw new IllegalArgumentException("Device type " + deviceType + " unknown");
	}

	public static void main(String[] args) throws IOException {
		/* Before this is run, be sure to set up the launch configuration (Arguments->VM Arguments)
		 * for the correct SWT library path in order to run with the SWT dlls. 
		 * The dlls are located in the SWT plugin jar.  
		 * For example, on Windows the Eclipse SWT 3.1 plugin jar is:
		 *       installation_directory\plugins\org.eclipse.swt.win32_3.1.0.jar
		 */

		if (args.length < 2) {
			System.err.println("Required parameters: <device type: 'parallel' or 'witilt-serial' or 'witilt-bluetooth'> <device1> <device2>");
			System.err.println("                     listentcp <tcp port>        ... assumes parallel");
			System.exit(1);
		}
		int deviceType = -1;
		String dev1 = null, dev2 = null;
		if (args[0].equals("parallel")) {
			deviceType = 1;
			dev1 = args[1];
		}
		else if (args[0].equals("witilt-serial")) {
			deviceType = 2;
			dev1 = args[1];
			dev2 = args[2];
		}
		else if (args[0].equals("witilt-bluetooth")) {
			deviceType = 3;
			dev1 = args[1];
			dev2 = args[2];
		}
		else if (args[0].equals("listentcp")) {
			deviceType = 1;
			dev1 = "port:" + args[1];
		}
		
		ShakingSinglePCDemonstrator thisClass = new ShakingSinglePCDemonstrator(dev1, dev2, deviceType);

		Display display = Display.getDefault();
		while (!thisClass.sShell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		
		// stop the sensor listener threads properly
		if (thisClass.reader1 != null)
			thisClass.reader1.stop();
		if (thisClass.reader2 != null)
			thisClass.reader2.stop();
		
		System.exit(0);
	}
	
	private class Protocol1Hooks extends ShakeWellBeforeUseProtocol1 {
		private final static boolean keepConnected = true;
		private final static boolean concurrentVerificationSupported = false;
		private final static boolean useJSSE = true;
		
		protected Protocol1Hooks() {
			// samplerate/2
			super(new TCPPortServer(ShakeWellBeforeUseProtocol1.TcpPort, 
					ShakeWellBeforeUseProtocol1.KeyAgreementProtocolTimeout,
					keepConnected, useJSSE), 
					keepConnected,keepConnected,  concurrentVerificationSupported,
					ShakeWellBeforeUseParameters.coherenceThreshold, 0.0, 
					ShakeWellBeforeUseParameters.coherenceWindowSize, useJSSE);
		}
		
		// TODO: activate me again when J2ME polish can deal with Java5 sources!
		//@Override
		protected void protocolSucceededHook(RemoteConnection remote, Object optionalVerificationId,
				String optionalParameterFromRemote, byte[] sharedSessionKey) {
			logger.info("Protocol variant 1 succedded with " + (remote != null ? remote.getRemoteName() : "null") + 
					": shared key is " + (sharedSessionKey != null ? sharedSessionKey.toString() : "null"));
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					coherenceField.setBackground(new Color(Display.getDefault(), 0, 255, 0));
					coherenceValue.setText(Double.toString(lastCoherenceMean));
				}
			});
		}		

		// TODO: activate me again when J2ME polish can deal with Java5 sources!
		//@Override
		protected void protocolFailedHook(RemoteConnection remote, Object optionalVerificationId, 
				Exception e, String message) {
			logger.info("Protocol variant 1 failed with " + remote  + ": " + e + ", " + message); 
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					if (lastCoherenceMean >= 0.4f)
						coherenceField.setBackground(new Color(Display.getDefault(), 255, 255, 0));
					else
						coherenceField.setBackground(new Color(Display.getDefault(), 255, 0, 0));
					coherenceValue.setText(Double.toString(lastCoherenceMean));
				}
			});
		}
		
		// TODO: activate me again when J2ME polish can deal with Java5 sources!
		//@Override
		protected void protocolProgressHook(RemoteConnection remote, 
				int cur, int max, String message) {
			logger.debug("Protocol variant 1 progress with " + remote +
					" " + cur + " of " + max + ": " + message); 
		}		
	}

	private class Protocol2Hooks extends ShakeWellBeforeUseProtocol2 {
		protected Protocol2Hooks(int numMatches, int udpRecvPort, int udpSendPort, String instanceId) throws IOException {
			super(ShakeWellBeforeUseParameters.samplerate, ShakeWellBeforeUseParameters.fftMatchesWindowSize,
					ShakeWellBeforeUseParameters.fftMatchesQuantizationLevels, ShakeWellBeforeUseParameters.fftMatchesCandidatesPerRound,
					ShakeWellBeforeUseParameters.fftMatchesCutOffFrequenecy, ShakeWellBeforeUseParameters.fftMatchesWindowOverlap,
					ShakeWellBeforeUseParameters.fftMatchesThreshold,
					numMatches, false, udpRecvPort, udpSendPort, "127.0.0.1", instanceId);
		}
		
		// TODO: activate me again when J2ME polish can deal with Java5 sources!
		//@Override
		protected void protocolSucceededHook(String remote, byte[] sharedSessionKey, float matchingRoundsFraction) {
			logger.info("Protocol variant 2 succedded with " + remote + " with " + 
					matchingRoundsFraction + " matching rounds: shared key is " + sharedSessionKey.toString());
			final float fraction = matchingRoundsFraction;
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					matchingField.setBackground(new Color(Display.getDefault(), 0, 255, 0));
					matchingValue.setText(Float.toString(fraction));
				}
			});
		}

		// TODO: activate me again when J2ME polish can deal with Java5 sources!
		//@Override
		protected void protocolFailedHook(String remote, float matchingRoundsFraction, Exception e, String message) {
			logger.info("Protocol variant 2 failed with " + remote + " with " + 
					matchingRoundsFraction + " matching rounds: " + e + ", " + message); 
			final float fraction = matchingRoundsFraction;
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					matchingField.setBackground(new Color(Display.getDefault(), 255, 0, 0));
					matchingValue.setText(Double.toString(fraction));
				}
			});
		}

		// TODO: activate me again when J2ME polish can deal with Java5 sources!
		//@Override
		protected void protocolProgressHook(String remote, int cur, int max, String message) {
			logger.debug("Protocol variant 2 progress with " + remote +
					" " + cur + " of " + max + ": " + message); 
		}
	}
	
	private class StateListener implements SamplesSink {
		/** The index in @see #deviceStates that this object is responsible for. */
		int deviceIndex;
		
		private StateListener(int deviceIndex) {
			this.deviceIndex = deviceIndex;
		}
		
		/** This implementation changes the display state of the respective device to active. */
		public void segmentStart(int numSample) {
			logger.debug("Device " + deviceIndex + " became active at sample " + numSample);
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					deviceStates[deviceIndex].setText(DEVICE_STATE_ACTIVE);
				}
			});
		}
		
		/** This implementation changes the display state of the respective device to quiescent. */
		public void segmentEnd(int numSample) {
			logger.debug("Device " + deviceIndex + " became qiescent at sample " + numSample);
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					deviceStates[deviceIndex].setText(DEVICE_STATE_QUIESCENT);
				}
			});
		}

		/** This method is a dummy implementation and does nothing. */ 
		public void addSample(double sample, int numSample) {
			// not interested in samples here, so warnings shut up
		}
	}
}
