package io.eiren.gui;

import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.event.MouseInputAdapter;

import dev.slimevr.gui.AutoBoneWindow;
import dev.slimevr.gui.swing.ButtonTimer;
import dev.slimevr.gui.swing.EJBagNoStretch;
import io.eiren.util.StringUtils;
import io.eiren.util.ann.ThreadSafe;
import io.eiren.vr.VRServer;
import io.eiren.vr.processor.HumanSkeleton;

public class SkeletonConfig extends EJBagNoStretch {

	private final VRServer server;
	private final VRServerGUI gui;
	private final AutoBoneWindow autoBone;
	private Map<String, SkeletonLabel> labels = new HashMap<>();

	public SkeletonConfig(VRServer server, VRServerGUI gui) {
		super(false, true);
		this.server = server;
		this.gui = gui;
		this.autoBone = new AutoBoneWindow(server, this);

		setAlignmentY(TOP_ALIGNMENT);
		server.humanPoseProcessor.addSkeletonUpdatedCallback(this::skeletonUpdated);
		skeletonUpdated(null);
	}

	@ThreadSafe
	public void skeletonUpdated(HumanSkeleton newSkeleton) {
		java.awt.EventQueue.invokeLater(() -> {
			removeAll();

			int row = 0;

			/**
			add(new JCheckBox("Extended pelvis model") {{
				addItemListener(new ItemListener() {
				    @Override
				    public void itemStateChanged(ItemEvent e) {
				        if(e.getStateChange() == ItemEvent.SELECTED) {//checkbox has been selected
				        	if(newSkeleton != null && newSkeleton instanceof HumanSkeletonWithLegs) {
				        		HumanSkeletonWithLegs hswl = (HumanSkeletonWithLegs) newSkeleton;
				        		hswl.setSkeletonConfigBoolean("Extended pelvis model", true);
				        	}
				        } else {
				        	if(newSkeleton != null && newSkeleton instanceof HumanSkeletonWithLegs) {
				        		HumanSkeletonWithLegs hswl = (HumanSkeletonWithLegs) newSkeleton;
				        		hswl.setSkeletonConfigBoolean("Extended pelvis model", false);
				        	}
				        }
				    }
				});
				if(newSkeleton != null && newSkeleton instanceof HumanSkeletonWithLegs) {
	        		HumanSkeletonWithLegs hswl = (HumanSkeletonWithLegs) newSkeleton;
	        		setSelected(hswl.getSkeletonConfigBoolean("Extended pelvis model"));
				}
			}}, s(c(0, row, 2), 3, 1));
			row++;
			//*/
			/*
			add(new JCheckBox("Extended knee model") {{
				addItemListener(new ItemListener() {
				    @Override
				    public void itemStateChanged(ItemEvent e) {
				        if(e.getStateChange() == ItemEvent.SELECTED) {//checkbox has been selected
				        	if(newSkeleton != null && newSkeleton instanceof HumanSkeletonWithLegs) {
				        		HumanSkeletonWithLegs hswl = (HumanSkeletonWithLegs) newSkeleton;
				        		hswl.setSkeletonConfigBoolean("Extended knee model", true);
				        	}
				        } else {
				        	if(newSkeleton != null && newSkeleton instanceof HumanSkeletonWithLegs) {
				        		HumanSkeletonWithLegs hswl = (HumanSkeletonWithLegs) newSkeleton;
				        		hswl.setSkeletonConfigBoolean("Extended knee model", false);
				        	}
				        }
				    }
				});
				if(newSkeleton != null && newSkeleton instanceof HumanSkeletonWithLegs) {
	        		HumanSkeletonWithLegs hswl = (HumanSkeletonWithLegs) newSkeleton;
	        		setSelected(hswl.getSkeletonConfigBoolean("Extended knee model"));
				}
			}}, s(c(0, row, 2), 3, 1));
			row++;
			//*/

			add(new TimedResetButton("重置全部", "All"), s(c(1, row, 2), 3, 1));
			add(new JButton("自动") {{
				addMouseListener(new MouseInputAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						autoBone.setVisible(true);
						autoBone.toFront();
					}
				});
			}}, s(c(4, row, 2), 3, 1));
			row++;

			add(new JLabel("胸部"), c(0, row, 2));
			add(new AdjButton("+", "胸部", 0.01f), c(1, row, 2));
			add(new SkeletonLabel("胸部"), c(2, row, 2));
			add(new AdjButton("-", "胸部", -0.01f), c(3, row, 2));
			add(new ResetButton("重置", "胸部"), c(4, row, 2));
			row++;

			add(new JLabel("腰部"), c(0, row, 2));
			add(new AdjButton("+", "腰部", 0.01f), c(1, row, 2));
			add(new SkeletonLabel("腰部"), c(2, row, 2));
			add(new AdjButton("-", "腰部", -0.01f), c(3, row, 2));
			add(new TimedResetButton("重置", "腰部"), c(4, row, 2));
			row++;

			add(new JLabel("臀部宽度"), c(0, row, 2));
			add(new AdjButton("+", "臀部宽度", 0.01f), c(1, row, 2));
			add(new SkeletonLabel("臀部宽度"), c(2, row, 2));
			add(new AdjButton("-", "臀部宽度", -0.01f), c(3, row, 2));
			add(new ResetButton("重置", "臀部宽度"), c(4, row, 2));
			row++;

			add(new JLabel("腿长"), c(0, row, 2));
			add(new AdjButton("+", "腿长", 0.01f), c(1, row, 2));
			add(new SkeletonLabel("腿长"), c(2, row, 2));
			add(new AdjButton("-", "腿长", -0.01f), c(3, row, 2));
			add(new TimedResetButton("重置", "腿长"), c(4, row, 2));
			row++;

			add(new JLabel("膝盖高度"), c(0, row, 2));
			add(new AdjButton("+", "膝盖高度", 0.01f), c(1, row, 2));
			add(new SkeletonLabel("膝盖高度"), c(2, row, 2));
			add(new AdjButton("-", "膝盖高度", -0.01f), c(3, row, 2));
			add(new TimedResetButton("重置", "膝盖高度"), c(4, row, 2));
			row++;

			add(new JLabel("足长"), c(0, row, 2));
			add(new AdjButton("+", "足长", 0.01f), c(1, row, 2));
			add(new SkeletonLabel("足长"), c(2, row, 2));
			add(new AdjButton("-", "足长", -0.01f), c(3, row, 2));
			add(new ResetButton("重置", "足长"), c(4, row, 2));
			row++;

			add(new JLabel("头部偏移"), c(0, row, 2));
			add(new AdjButton("+", "头部", 0.01f), c(1, row, 2));
			add(new SkeletonLabel("头部"), c(2, row, 2));
			add(new AdjButton("-", "头部", -0.01f), c(3, row, 2));
			add(new ResetButton("重置", "头部"), c(4, row, 2));
			row++;

			add(new JLabel("脖子长度"), c(0, row, 2));
			add(new AdjButton("+", "脖子", 0.01f), c(1, row, 2));
			add(new SkeletonLabel("脖子"), c(2, row, 2));
			add(new AdjButton("-", "脖子", -0.01f), c(3, row, 2));
			add(new ResetButton("重置", "脖子"), c(4, row, 2));
			row++;

			add(new JLabel("虚拟腰部"), c(0, row, 2));
			add(new AdjButton("+", "虚拟腰部", 0.01f), c(1, row, 2));
			add(new SkeletonLabel("虚拟腰部"), c(2, row, 2));
			add(new AdjButton("-", "虚拟腰部", -0.01f), c(3, row, 2));
			add(new ResetButton("重置", "虚拟腰部"), c(4, row, 2));
			row++;

			gui.refresh();
		});
	}

	@ThreadSafe
	public void refreshAll() {
		java.awt.EventQueue.invokeLater(() -> {
			labels.forEach((joint, label) -> {
				label.setText(StringUtils.prettyNumber(server.humanPoseProcessor.getSkeletonConfig(joint) * 100, 0));
			});
		});
	}

	private void change(String joint, float diff) {
		float current = server.humanPoseProcessor.getSkeletonConfig(joint);
		server.humanPoseProcessor.setSkeletonConfig(joint, current + diff);
		server.saveConfig();
		labels.get(joint).setText(StringUtils.prettyNumber((current + diff) * 100, 0));
	}

	private void reset(String joint) {
		server.humanPoseProcessor.resetSkeletonConfig(joint);
		server.saveConfig();
		if(!"All".equals(joint)) {
			float current = server.humanPoseProcessor.getSkeletonConfig(joint);
			labels.get(joint).setText(StringUtils.prettyNumber((current) * 100, 0));
		} else {
			labels.forEach((jnt, label) -> {
				float current = server.humanPoseProcessor.getSkeletonConfig(jnt);
				label.setText(StringUtils.prettyNumber((current) * 100, 0));
			});
		}
	}

	private class SkeletonLabel extends JLabel {

		public SkeletonLabel(String joint) {
			super(StringUtils.prettyNumber(server.humanPoseProcessor.getSkeletonConfig(joint) * 100, 0));
			labels.put(joint, this);
		}
	}

	private class AdjButton extends JButton {

		public AdjButton(String text, String joint, float diff) {
			super(text);
			addMouseListener(new MouseInputAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					change(joint, diff);
				}
			});
		}
	}

	private class ResetButton extends JButton {

		public ResetButton(String text, String joint) {
			super(text);
			addMouseListener(new MouseInputAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					reset(joint);
				}
			});
		}
	}

	private class TimedResetButton extends JButton {

		public TimedResetButton(String text, String joint) {
			super(text);
			addMouseListener(new MouseInputAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					ButtonTimer.runTimer(TimedResetButton.this, 3, text, () -> reset(joint));
				}
			});
		}
	}
}
