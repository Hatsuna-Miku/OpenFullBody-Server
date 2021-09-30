package dev.slimevr.gui;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import io.eiren.gui.SkeletonConfig;
import io.eiren.util.StringUtils;
import io.eiren.util.ann.AWTThread;
import io.eiren.util.collections.FastList;
import io.eiren.util.logging.LogManager;
import io.eiren.vr.VRServer;

import javax.swing.event.MouseInputAdapter;

import org.apache.commons.lang3.tuple.Pair;

import dev.slimevr.autobone.AutoBone;
import dev.slimevr.gui.swing.EJBox;
import dev.slimevr.poserecorder.PoseFrame;
import dev.slimevr.poserecorder.PoseFrameIO;
import dev.slimevr.poserecorder.PoseRecorder;

public class AutoBoneWindow extends JFrame {
	
	private static File saveDir = new File("Recordings");
	private static File loadDir = new File("LoadRecordings");
	
	private EJBox pane;
	
	private final transient VRServer server;
	private final transient SkeletonConfig skeletonConfig;
	private final transient PoseRecorder poseRecorder;
	private final transient AutoBone autoBone;
	
	private transient Thread recordingThread = null;
	private transient Thread saveRecordingThread = null;
	private transient Thread autoBoneThread = null;
	
	private JButton saveRecordingButton;
	private JButton adjustButton;
	private JButton applyButton;
	
	private JLabel processLabel;
	private JLabel lengthsLabel;
	
	public AutoBoneWindow(VRServer server, SkeletonConfig skeletonConfig) {
		super("自动骨骼绑定");
		
		this.server = server;
		this.skeletonConfig = skeletonConfig;
		this.poseRecorder = new PoseRecorder(server);
		this.autoBone = new AutoBone(server);
		
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		add(new JScrollPane(pane = new EJBox(BoxLayout.PAGE_AXIS), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		
		build();
	}
	
	private String getLengthsString() {
		boolean first = true;
		StringBuilder configInfo = new StringBuilder("");
		for(Entry<String, Float> entry : autoBone.configs.entrySet()) {
			if(!first) {
				configInfo.append(", ");
			} else {
				first = false;
			}
			
			configInfo.append(entry.getKey() + ": " + StringUtils.prettyNumber(entry.getValue() * 100f, 2));
		}
		
		return configInfo.toString();
	}
	
	private void saveRecording(PoseFrame frames) {
		if(saveDir.isDirectory() || saveDir.mkdirs()) {
			File saveRecording;
			int recordingIndex = 1;
			do {
				saveRecording = new File(saveDir, "ABRecording" + recordingIndex++ + ".pfr");
			} while(saveRecording.exists());
			
			LogManager.log.info("[AutoBone] 关键帧导出至 \"" + saveRecording.getPath() + "\"...");
			if(PoseFrameIO.writeToFile(saveRecording, frames)) {
				LogManager.log.info("[AutoBone] 导出完成，已存放至以下文件夹 \"" + saveRecording.getPath() + "\"。");
			} else {
				LogManager.log.severe("[AutoBone] 导出录制到 \"" + saveRecording.getPath() + "\"失败。");
			}
		} else {
			LogManager.log.severe("[AutoBone] 在以下文件夹创建录制目录 \"" + saveDir.getPath() + "\"失败。");
		}
	}
	
	private List<Pair<String, PoseFrame>> loadRecordings() {
		List<Pair<String, PoseFrame>> recordings = new FastList<Pair<String, PoseFrame>>();
		if(loadDir.isDirectory()) {
			File[] files = loadDir.listFiles();
			if(files != null) {
				for(File file : files) {
					if(file.isFile() && org.apache.commons.lang3.StringUtils.endsWithIgnoreCase(file.getName(), ".pfr")) {
						LogManager.log.info("[AutoBone] 检测到录制记录于 \"" + file.getPath() + "\"，加载关键帧中...");
						PoseFrame frames = PoseFrameIO.readFromFile(file);
						
						if(frames == null) {
							LogManager.log.severe("从 \"" + file.getPath() + "\" 中读取关键帧失败...");
						} else {
							recordings.add(Pair.of(file.getName(), frames));
						}
					}
				}
			}
		}
		
		return recordings;
	}
	
	private float processFrames(PoseFrame frames) {
		autoBone.minDataDistance = server.config.getInt("autobone.minimumDataDistance", autoBone.minDataDistance);
		autoBone.maxDataDistance = server.config.getInt("autobone.maximumDataDistance", autoBone.maxDataDistance);
		
		autoBone.numEpochs = server.config.getInt("autobone.epochCount", autoBone.numEpochs);
		
		autoBone.initialAdjustRate = server.config.getFloat("autobone.adjustRate", autoBone.initialAdjustRate);
		autoBone.adjustRateDecay = server.config.getFloat("autobone.adjustRateDecay", autoBone.adjustRateDecay);
		
		autoBone.slideErrorFactor = server.config.getFloat("autobone.slideErrorFactor", autoBone.slideErrorFactor);
		autoBone.offsetErrorFactor = server.config.getFloat("autobone.offsetErrorFactor", autoBone.offsetErrorFactor);
		autoBone.proportionErrorFactor = server.config.getFloat("autobone.proportionErrorFactor", autoBone.proportionErrorFactor);
		autoBone.heightErrorFactor = server.config.getFloat("autobone.heightErrorFactor", autoBone.heightErrorFactor);
		autoBone.positionErrorFactor = server.config.getFloat("autobone.positionErrorFactor", autoBone.positionErrorFactor);
		autoBone.positionOffsetErrorFactor = server.config.getFloat("autobone.positionOffsetErrorFactor", autoBone.positionOffsetErrorFactor);
		
		boolean calcInitError = server.config.getBoolean("autobone.calculateInitialError", true);
		float targetHeight = server.config.getFloat("autobone.manualTargetHeight", -1f);
		return autoBone.processFrames(frames, calcInitError, targetHeight, (epoch) -> {
			processLabel.setText(epoch.toString());
			lengthsLabel.setText(getLengthsString());
		});
	}
	
	@AWTThread
	private void build() {
		pane.add(new EJBox(BoxLayout.LINE_AXIS) {
			{
				setBorder(new EmptyBorder(i(5)));
				add(new JButton("开始录制") {
					{
						addMouseListener(new MouseInputAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								// Prevent running multiple times
								if(!isEnabled() || recordingThread != null) {
									return;
								}
								
								Thread thread = new Thread() {
									@Override
									public void run() {
										try {
											if(poseRecorder.isReadyToRecord()) {
												setText("录制中...");
												// 1000 samples at 20 ms per sample is 20 seconds
												int sampleCount = server.config.getInt("autobone.sampleCount", 1000);
												long sampleRate = server.config.getLong("autobone.sampleRateMs", 20L);
												Future<PoseFrame> framesFuture = poseRecorder.startFrameRecording(sampleCount, sampleRate);
												PoseFrame frames = framesFuture.get();
												LogManager.log.info("[AutoBone] 录制完成！");
												
												saveRecordingButton.setEnabled(true);
												adjustButton.setEnabled(true);
												
												if(server.config.getBoolean("autobone.saveRecordings", false)) {
													setText("保存中...");
													saveRecording(frames);
												}
											} else {
												setText("未完成...");
												LogManager.log.severe("[AutoBone] 无法录制...");
												Thread.sleep(3000); // Wait for 3 seconds
												return;
											}
										} catch(Exception e) {
											setText("录制失败...");
											LogManager.log.severe("[AutoBone] 录制失败！", e);
											try {
												Thread.sleep(3000); // Wait for 3 seconds
											} catch(Exception e1) {
												// Ignore
											}
										} finally {
											setText("正在开始录制");
											recordingThread = null;
										}
									}
								};
								
								recordingThread = thread;
								thread.start();
							}
						});
					}
				});
				
				add(saveRecordingButton = new JButton("保存记录") {
					{
						setEnabled(poseRecorder.hasRecording());
						addMouseListener(new MouseInputAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								// Prevent running multiple times
								if(!isEnabled() || saveRecordingThread != null) {
									return;
								}
								
								Thread thread = new Thread() {
									@Override
									public void run() {
										try {
											Future<PoseFrame> framesFuture = poseRecorder.getFramesAsync();
											if(framesFuture != null) {
												setText("正在等待录制...");
												PoseFrame frames = framesFuture.get();
												
												if(frames.getTrackerCount() <= 0) {
													throw new IllegalStateException("无追踪器");
												}
												
												if(frames.getMaxFrameCount() <= 0) {
													throw new IllegalStateException("无关键帧");
												}
												
												setText("保存中...");
												saveRecording(frames);
												
												setText("保存成功！");
												try {
													Thread.sleep(3000); // Wait for 3 seconds
												} catch(Exception e1) {
													// Ignore
												}
											} else {
												setText("未在录制...");
												LogManager.log.severe("[AutoBone] 无法保存，未能完成录制...");
												try {
													Thread.sleep(3000); // Wait for 3 seconds
												} catch(Exception e1) {
													// Ignore
												}
												return;
											}
										} catch(Exception e) {
											setText("保存失败...");
											LogManager.log.severe("[AutoBone] 记录保存失败！", e);
											try {
												Thread.sleep(3000); // Wait for 3 seconds
											} catch(Exception e1) {
												// Ignore
											}
										} finally {
											setText("保存记录");
											saveRecordingThread = null;
										}
									}
								};
								
								saveRecordingThread = thread;
								thread.start();
							}
						});
					}
				});
				
				add(adjustButton = new JButton("自动调整") {
					{
						// If there are files to load, enable the button
						setEnabled(poseRecorder.hasRecording() || (loadDir.isDirectory() && loadDir.list().length > 0));
						addMouseListener(new MouseInputAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								// Prevent running multiple times
								if(!isEnabled() || autoBoneThread != null) {
									return;
								}
								
								Thread thread = new Thread() {
									@Override
									public void run() {
										try {
											setText("加载中...");
											List<Pair<String, PoseFrame>> frameRecordings = loadRecordings();
											
											if(!frameRecordings.isEmpty()) {
												LogManager.log.info("[AutoBone] 关键帧加载完成！");
											} else {
												Future<PoseFrame> framesFuture = poseRecorder.getFramesAsync();
												if(framesFuture != null) {
													setText("等待录制中...");
													PoseFrame frames = framesFuture.get();
													
													if(frames.getTrackerCount() <= 0) {
														throw new IllegalStateException("无追踪器");
													}
													
													if(frames.getMaxFrameCount() <= 0) {
														throw new IllegalStateException("无关键帧");
													}
													
													frameRecordings.add(Pair.of("<录制中>", frames));
												} else {
													setText("无录制记录...");
													LogManager.log.severe("[AutoBone] 目录 \"" + loadDir.getPath() + "\" 下未发现关键帧，录制未完成...");
													try {
														Thread.sleep(3000); // Wait for 3 seconds
													} catch(Exception e1) {
														// Ignore
													}
													return;
												}
											}
											
											setText("处理中..");
											LogManager.log.info("[AutoBone] 正在处理关键帧...");
											FastList<Float> heightPercentError = new FastList<Float>(frameRecordings.size());
											for(Pair<String, PoseFrame> recording : frameRecordings) {
												LogManager.log.info("[AutoBone] 正在处理来自 \"" + recording.getKey() + "\"的关键帧...");
												
												heightPercentError.add(processFrames(recording.getValue()));
												LogManager.log.info("[AutoBone] 处理完成！");
												applyButton.setEnabled(true);
												
												//#region Stats/Values
												Float neckLength = autoBone.getConfig("Neck");
												Float chestLength = autoBone.getConfig("Chest");
												Float waistLength = autoBone.getConfig("Waist");
												Float hipWidth = autoBone.getConfig("Hips width");
												Float legsLength = autoBone.getConfig("Legs length");
												Float kneeHeight = autoBone.getConfig("Knee height");
												
												float neckWaist = neckLength != null && waistLength != null ? neckLength / waistLength : 0f;
												float chestWaist = chestLength != null && waistLength != null ? chestLength / waistLength : 0f;
												float hipWaist = hipWidth != null && waistLength != null ? hipWidth / waistLength : 0f;
												float legWaist = legsLength != null && waistLength != null ? legsLength / waistLength : 0f;
												float legBody = legsLength != null && waistLength != null && neckLength != null ? legsLength / (waistLength + neckLength) : 0f;
												float kneeLeg = kneeHeight != null && legsLength != null ? kneeHeight / legsLength : 0f;
												
												LogManager.log.info("[AutoBone] 系数：[{脖子-腰部: " + StringUtils.prettyNumber(neckWaist) + "}, {胸-腰部：" + StringUtils.prettyNumber(chestWaist) + "}, {臀-腰部：" + StringUtils.prettyNumber(hipWaist) + "}, {大腿-腰部：" + StringUtils.prettyNumber(legWaist) + "}, {大腿-身体：" + StringUtils.prettyNumber(legBody) + "}, {膝盖-大腿：" + StringUtils.prettyNumber(kneeLeg) + "}]");
												
												String lengthsString = getLengthsString();
												LogManager.log.info("[AutoBone] 长度：" + lengthsString);
												lengthsLabel.setText(lengthsString);
											}
											
											if(!heightPercentError.isEmpty()) {
												float mean = 0f;
												for(float val : heightPercentError) {
													mean += val;
												}
												mean /= heightPercentError.size();
												
												float std = 0f;
												for(float val : heightPercentError) {
													float stdVal = val - mean;
													std += stdVal * stdVal;
												}
												std = (float) Math.sqrt(std / heightPercentError.size());
												
												LogManager.log.info("[AutoBone] 平均高度错误！" + StringUtils.prettyNumber(mean, 6) + " (SD " + StringUtils.prettyNumber(std, 6) + ")");
											}
											//#endregion
										} catch(Exception e) {
											setText("失败...");
											LogManager.log.severe("[AutoBone] 调整失败！", e);
											try {
												Thread.sleep(3000); // Wait for 3 seconds
											} catch(Exception e1) {
												// Ignore
											}
										} finally {
											setText("自动调整");
											autoBoneThread = null;
										}
									}
								};
								
								autoBoneThread = thread;
								thread.start();
							}
						});
					}
				});
				
				add(applyButton = new JButton("应用数值") {
					{
						setEnabled(false);
						addMouseListener(new MouseInputAdapter() {
							@Override
							public void mouseClicked(MouseEvent e) {
								if(!isEnabled()) {
									return;
								}
								
								autoBone.applyConfig();
								// Update GUI values after applying
								skeletonConfig.refreshAll();
							}
						});
					}
				});
			}
		});
		
		pane.add(new EJBox(BoxLayout.LINE_AXIS) {
			{
				setBorder(new EmptyBorder(i(5)));
				add(processLabel = new JLabel("还未开始处理..."));
			}
		});
		
		pane.add(new EJBox(BoxLayout.LINE_AXIS) {
			{
				setBorder(new EmptyBorder(i(5)));
				add(lengthsLabel = new JLabel(getLengthsString()));
			}
		});
		
		// Pack and display
		pack();
		setLocationRelativeTo(null);
		setVisible(false);
	}
}
