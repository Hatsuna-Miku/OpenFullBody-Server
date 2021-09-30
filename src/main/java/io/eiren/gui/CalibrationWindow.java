package io.eiren.gui;


import java.awt.Container;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;

import dev.slimevr.gui.swing.EJBox;
import io.eiren.util.ann.AWTThread;
import io.eiren.vr.trackers.CalibratingTracker;
import io.eiren.vr.trackers.Tracker;

public class CalibrationWindow extends JFrame {
	
	public final Tracker tracker;
	private JTextArea currentCalibration;
	private JTextArea newCalibration;
	private JButton calibrateButton;
	
	public CalibrationWindow(Tracker t) {
		super(t.getName() + " 校准");
		this.tracker = t;
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.LINE_AXIS));
		
		build();
	}
	
	public void currentCalibrationRecieved(String str) {
		java.awt.EventQueue.invokeLater(() -> {
			currentCalibration.setText(str);
			pack();
		});
	}
	
	public void newCalibrationRecieved(String str) {
		java.awt.EventQueue.invokeLater(() -> {
			calibrateButton.setText("校准");
			newCalibration.setText(str);
			pack();
		});
	}
	
	@AWTThread
	private void build() {
		Container pane = getContentPane();
		
		pane.add(calibrateButton = new JButton("校准") {{
			addMouseListener(new MouseInputAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					calibrateButton.setText("校准中...");
					((CalibratingTracker) tracker).startCalibration(CalibrationWindow.this::newCalibrationRecieved);
				}
			});
		}});

		pane.add(new EJBox(BoxLayout.PAGE_AXIS) {{
			setBorder(new EmptyBorder(i(5)));
			add(new JLabel("现有校准数据"));
			add(currentCalibration = new JTextArea(10, 25));
			
			((CalibratingTracker) tracker).requestCalibrationData(CalibrationWindow.this::currentCalibrationRecieved);
		}});
		pane.add(new EJBox(BoxLayout.PAGE_AXIS) {{
			setBorder(new EmptyBorder(i(5)));
			add(new JLabel("新建校准数据"));
			add(newCalibration = new JTextArea(10, 25));
		}});
		
		
		// Pack and display
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				toFront();
				repaint();
			}
		});
	}
}
