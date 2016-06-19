package picture;

import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacpp.opencv_imgcodecs;

import app.CommandController;
import helper.Command;
import helper.CustomPoint;

public class ScanSequence implements Runnable {
	private static final int MIN_HIT_COUNT = 6;
	private static final int FORWARD_TIME_2 = 800;
	private static final int BACKWARD_TIME = 1500;
	private static final int BACKWARD_SPEED = 9;
	private static final int STRAFE_TIME = 1000;
	private static final int STRAFE_SPEED = 7;
	private static final int SPIN_TIME = 1000;
	private static final int SPIN_SPEED = 15;
	private static final int FORWARD_TIME = 800;
	private static final int FORWARD_SPEED = 7;
	private static final int ROTATE_TIME = 1500;
	private static final int ROTATE_SPEED = 25;
	private static final int FIELD_DURATION = 1500;
	private static final int FIELD_SPEED = 7;
	private static final int FIELD_SMALL_DURATION = 800;
	private static final int FIELD_SMALL_SPEED = 6;
	private static final int MAX_Y_CORD = 1060;
	private static final int MIN_Y_CORD = -10;
	private static final int MAX_X_CORD = 926;
	private static final int MIN_X_CORD = 80;
	private static int noDistFound = 0;
	private static final int noDistFoundLimit = 200;
	private static final int MAX_FRAME_COUNT = 8;
	private static final int MAX_ROTATE_COUNT = 15;

	// #TODO Tweak these values based on testing
	public static final double CENTER_UPPER = 0.15;
	public static final double CENTER_LOWER = -0.15;
	public static final double CENTER_DIFFERENCE = 0.05;
	public static final double SMALL_MOVE_LIMIT = 100;

	private CommandController commandController;
	private double chunkSize = 0;
	private double previousCenter = -1;
	private boolean strafeRight = true;
	private String code = null;
	private int rotateCount = 0;
	private int frameCount = 0;
	private int scannedCount = 0;
	private Mat camMat;
	private int foundFrameCount = 0;
	private boolean moveToStart = false;
	private int endX = 847;
	private int endY = -10;
	private CustomPoint endPlacement;
	private PictureProcessingHelper pictureProcessingHelper = new PictureProcessingHelper();
	private Map<Integer, Integer> moves = new HashMap<>();
	private boolean isFirst = false;
	private boolean manualUpdateX = true;
	private final static int CHUNK_SIZE = 70;
	private double distanceFromQr = 0;
	private boolean moveX;
	private boolean doCommand = false;
	private boolean startSmallMove = false;
	private boolean canGetDist = true;
	private boolean noMove = false;
	private double prevDistance = 0;
	private boolean runScanSequence = true;
	private boolean xDone = false;
	private boolean yDone = false;

	public ScanSequence(CommandController commandController) {
		this.commandController = commandController;
	}

	public void setImage(Mat camMat) {
		this.camMat = camMat;
	}

	@Override
	public void run() {
		commandController.droneInterface.takeOff();
		sleep(2000);
		System.out.println("HOVER");
		commandController.droneInterface.hover();
		sleep(2000);
		System.out.println("UP");
		commandController.addCommand(Command.UP, 2500, 12);

		while (runScanSequence) {
			if (OFVideo.imageChanged) {
				scanSequence();
			} else {
				sleep(50);
			}
		}

		firstAxisToMove();
		System.out.println("LETS GOOOOOOO MOTHERFUCKER");
		frameCount = 0;

		while (moveToStart) {
			moveDroneToPlacement(new CustomPoint(847, 50));
		}
		System.out.println("START THE CUDE SEQUENCE");

	}

	private void firstAxisToMove() {
		if (code.contains("W02") || code.contains("W00")) {
			moveX = false;
		}
		if (code.contains("W03") || code.contains("W01")) {
			moveX = true;
		}
	}

	private int getCorrectXMove() {
		if (code.contains("W02")) {
			return Command.LEFT;
		}
		if (code.contains("W03")) {
			return Command.BACKWARDS;
		}
		if (code.contains("W00")) {
			return Command.RIGHT;
		}
		if (code.contains("W01")) {
			return Command.FORWARD;
		}
		return Command.NONE;

	}

	private int getCorrectYMove(double y) {
		if (code.contains("W02")) {
			return Command.FORWARD;
		}
		if (code.contains("W03")) {
			return Command.LEFT;
		}
		if (code.contains("W00")) {
			return Command.BACKWARDS;
		}
		if (code.contains("W01")) {
			return Command.RIGHT;
		}

		return Command.NONE;

	}

	private void rotateCheck() {

		if (frameCount < MAX_FRAME_COUNT) {
			frameCount++;
		} else {
			code = null;
			if (rotateCount < MAX_ROTATE_COUNT) {
				addCommand(Command.ROTATERIGHT, ROTATE_TIME, ROTATE_SPEED);
				rotateCount++;
			} else {
				rotateCount = 0;
			}
			frameCount = 0;
		}
	}

	private RotatedRect mostCenteredRect(List<Mat> contours) {
		double distanceFomCenter = Double.MAX_VALUE;
		RotatedRect rect = new RotatedRect();
		for (int i = 0; i < contours.size(); i++) {
			RotatedRect rect2 = minAreaRect(contours.get(i));
			double distance = (camMat.arrayWidth() / 2) - rect2.center().x();
			if (distanceFomCenter > distance) {
				distanceFomCenter = Math.abs(distance);
				rect = rect2;
			}
		}
		return rect;
	}

	private void centerCheck(double center) {
		if (strafeRight) {
			addCommand(Command.RIGHT, STRAFE_TIME, STRAFE_SPEED);
		} else {
			addCommand(Command.LEFT, STRAFE_TIME, STRAFE_SPEED);
		}
		if (previousCenter == -1) {
			previousCenter = center;
		} else {
			double difference = center - previousCenter;
			if (difference > CENTER_DIFFERENCE) {
				strafeRight = !strafeRight;
				previousCenter = center;
				System.out.println("CHANGE STRAFE DIRECTION");
			}
		}
	}

	private void spinCheck(double positionFromCenter) {
		if (positionFromCenter > 0) {
			addCommand(Command.SPINRIGHT, SPIN_TIME, SPIN_SPEED);
		} else {
			addCommand(Command.SPINLEFT, SPIN_TIME, SPIN_SPEED);
		}
	}

	public int getSpinSpeed(List<Mat> matContours) {
		double allSpeeds = 0;
		double constant = 9;
		for (int i = 0; i < matContours.size(); i++) {
			double area = contourArea(matContours.get(i)) / 1000;
			double result = constant / area;
			allSpeeds += result * 10;
		}
		if (!Double.isNaN(allSpeeds / matContours.size())) {
			return (int) (allSpeeds / matContours.size());
		}
		return 0;
	}

	private void scanSequence() {
		OFVideo.imageChanged = false;
		List<Mat> contours = pictureProcessingHelper.findQrContours(camMat);
		if (contours.size() == 0) {
			rotateCheck();
			return;
		}
		foundFrameCount = 0;
		frameCount = 0;
		RotatedRect rect = mostCenteredRect(contours);

		double positionFromCenter = pictureProcessingHelper.isCenterInImage(camMat.clone(), rect);
		if (positionFromCenter != 0) {
			spinCheck(positionFromCenter);
			return;
		}
		double center = pictureProcessingHelper.center(rect);
		if (center > CENTER_UPPER || center < CENTER_LOWER) {
			centerCheck(center);
			return;
		}
		previousCenter = -1;

		Mat qrImg = pictureProcessingHelper.warpImage(camMat.clone(), rect);
		String tempCode = pictureProcessingHelper.scanQrCode(qrImg);
		if (tempCode != null) {
			code = tempCode;
			System.out.println(tempCode);
		}
		System.out.println("CENTERED");
		if (code != null) {
			if (code.contains("W01") || code.contains("W03")) {
				addCommand(Command.ROTATERIGHT, ROTATE_TIME, ROTATE_SPEED);
				return;
			} else if (contours.size() == 3) {
				PictureController.setPlacement(calculatePlacement(camMat, contours));
				moveToStart = true;
				rotateCount = 0;
				runScanSequence = false;

				return;
			} else if (scannedCount < 3) {
				scannedCount++;
				return;
			} else {
				scannedCount = 0;
				// #TODO Fly backwards (0.5 meters)
				addCommand(Command.BACKWARDS, BACKWARD_TIME, BACKWARD_SPEED);
				return;
			}
		} else {
			double distanceToSquare = pictureProcessingHelper.calcDistance(rect);
			if (distanceToSquare > 300) {
				addCommand(Command.FORWARD, FIELD_DURATION, FIELD_SPEED);
				return;
			} else {
				return;
			}
		}
	}

	private void sleep(int duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void addCommand(int task, int duration, int speed) {
		if (commandController.isDroneReady()) {
			if (moves.containsKey(task)) {
				if ((task == Command.SPINLEFT || task == Command.SPINRIGHT) && moves.get(task) > MIN_HIT_COUNT / 2) {
					commandController.addCommand(task, duration, speed);
					moves.clear();
				} else if (moves.get(task) > MIN_HIT_COUNT) {
					commandController.addCommand(task, duration, speed);
					moves.clear();
				} else {
					moves.put(task, moves.get(task) + 1);
				}
			} else {
				moves.put(task, 1);
			}
		}
	}

	private void addCommandForPlacement(int task, int duration, int speed) {
		if (commandController.isDroneReady()) {
			if (moves.containsKey(task)) {
				if ((task == Command.SPINLEFT || task == Command.SPINRIGHT) && moves.get(task) > MIN_HIT_COUNT / 2) {
					commandController.addCommand(task, duration, speed);
					moves.clear();
				} else if (moves.get(task) > MIN_HIT_COUNT) {
					updateRelativeCord(task);
					commandController.addCommand(task, duration, speed);
					moves.clear();
				} else {
					moves.put(task, moves.get(task) + 1);
				}
			} else {
				moves.put(task, 1);
			}
		}
	}

	private CustomPoint calculatePlacement(Mat srcImage, List<Mat> contours) {
		CustomPoint placement = new CustomPoint();
		List<Double> positions = new ArrayList<>();
		for (Mat mat : contours) {
			positions.add(pictureProcessingHelper.isCenterInImage(srcImage, minAreaRect(mat)));
		}
		int minIndex = positions.indexOf(Collections.min(positions));
		int maxIndex = positions.indexOf(Collections.max(positions));
		int middleIndex = 3 - minIndex - maxIndex;

		RotatedRect leftQR = minAreaRect(contours.get(minIndex));
		RotatedRect middleQR = minAreaRect(contours.get(middleIndex));
		RotatedRect rightQR = minAreaRect(contours.get(maxIndex));

		CustomPoint[] points = pictureProcessingHelper.calcPosition(pictureProcessingHelper.calcDistance(leftQR),
				pictureProcessingHelper.calcDistance(middleQR), pictureProcessingHelper.calcDistance(rightQR), code);
		CustomPoint scannedPoint = CustomPoint.parseQRText(code);
		for (CustomPoint e : points) {
			System.out.println(e.toString());
			if (Math.round(e.getX()) != scannedPoint.getX() || Math.round(e.getY()) != scannedPoint.getY()) {
				placement = e;
			}
		}

		return placement;
	}

	private void moveDroneToPlacement(CustomPoint placement) {
		endPlacement = placement;
		OFVideo.imageChanged = false;
		doCommand = false;
		List<Mat> contours = pictureProcessingHelper.findQrContours(camMat);
		// find mostcenteredrect
		RotatedRect rect = mostCenteredRect(contours);
		if (frameCount >= MAX_FRAME_COUNT) {
			frameCount = 0;
			distanceFromQr = pictureProcessingHelper.calcDistance(rect);
			// if (!Double.isInfinite(distanceFromQr) ||
			// !Double.isNaN(distanceFromQr)) {
			// prevDistance = distanceFromQr;
			// if (chunkSize == 0) {
			// chunkSize = distanceFromQr;
			// } else {
			// chunkSize = distanceFromQr - chunkSize;
			// }
			// }
			chunkSize = 55;
			// System.out.println("CHUNK " + chunkSize);
			// System.out.println("DISTANCE FROM QR" + distanceFromQr);
			// System.out.println("PREVDISTANCE" + prevDistance);

			// if no distances has been measured for a long time

			// Start moving
			if (moveX && !xDone) {
				int move = calcMoveXAxis(PictureController.getPlacement().getX(), placement);
				double positionFromCenter = pictureProcessingHelper.isCenterInImage(camMat.clone(), rect);
				if (positionFromCenter != 0) {
					spinCheck(positionFromCenter);
					return;
				}
				decideMove(move);
			} else {
				if (!yDone) {
					double positionFromCenter = pictureProcessingHelper.isCenterInImage(camMat.clone(), rect);
					if (positionFromCenter != 0) {
						spinCheck(positionFromCenter);
						return;
					}
					int move = calcMovesYAxis(PictureController.getPlacement().getY(), placement);
					decideMove(move);
				}
			}
		} else {
			frameCount++;
		}
	}

	private void updateRelativeCord(int move) {
		CustomPoint placement = PictureController.getPlacement();
		if (move == Command.BACKWARDS) {
			if (code.contains("W00")) {
				placement.setY(placement.getY() - chunkSize);
			} else if (code.contains("W01")) {
				placement.setX(placement.getX() - chunkSize);
			} else if (code.contains("W02")) {
				placement.setY(placement.getY() + chunkSize);
			} else {
				placement.setX(placement.getX() + chunkSize);
			}
		} else if (move == Command.FORWARD) {
			if (code.contains("W00")) {
				placement.setY(placement.getY() + chunkSize);
			} else if (code.contains("W01")) {
				placement.setX(placement.getX() + chunkSize);
			} else if (code.contains("W02")) {
				placement.setY(placement.getY() - chunkSize);
			} else {
				placement.setX(placement.getX() - chunkSize);
			}
		} else if (move == Command.RIGHT) {
			if (code.contains("W00")) {
				placement.setX(placement.getX() + chunkSize);
			} else if (code.contains("W01")) {
				placement.setY(placement.getY() - chunkSize);
			} else if (code.contains("W02")) {
				placement.setX(placement.getX() - chunkSize);
			} else {
				placement.setY(placement.getY() + chunkSize);
			}
		} else if (move == Command.LEFT) {
			if (code.contains("W00")) {
				placement.setX(placement.getX() - chunkSize);
			} else if (code.contains("W01")) {
				placement.setY(placement.getY() + chunkSize);
			} else if (code.contains("W02")) {
				placement.setX(placement.getX() - chunkSize);
			} else {
				placement.setY(placement.getY() - chunkSize);
			}
		}
		double differenceY = Math.abs((placement.getY() - endPlacement.getY()));
		boolean endYCondition = differenceY > 0 && differenceY < 60;

		double differenceX = Math.abs((placement.getX() - endPlacement.getX()));
		boolean endXCondition = differenceX > 0 && differenceX < 60;

		if (endYCondition && endXCondition) {
			moveToStart = false;
		}

		yDone = endYCondition;
		xDone = endXCondition;

		if (yDone) {
			System.out.println("Y COORD DONE");
			moveX = true;
		}

		if (xDone) {
			System.out.println("X COORD DONE");
			moveX = false;
		}
		// if (!Double.isInfinite(distanceFromQr)) {
		// if (placement.getX() >= 500 && placement.getY() >= 500) {
		// if (!moveX) {
		// if (code.contains("W00")) {
		// placement.setY(MAX_Y_CORD - distanceFromQr);
		// } else {
		// placement.setY(MIN_Y_CORD + distanceFromQr);
		// }
		//
		// } else {
		// if (code.contains("W01")) {
		// placement.setX(MAX_X_CORD - distanceFromQr);
		// } else {
		// placement.setX(MIN_X_CORD + distanceFromQr);
		// }
		// }
		// noMove = false;
		// }
		// }

		System.out.println(placement.toString());
		PictureController.setPlacement(placement);
	}

	private void decideMove(int move) {
		if (move == Command.LEFT || move == Command.RIGHT) {
			addCommandForPlacement(move, STRAFE_TIME, SPIN_SPEED);
		} else {
			addCommandForPlacement(move, FIELD_DURATION, FIELD_SPEED);
		}
	}

	private int calcMoveXAxis(double x, CustomPoint placement) {
		if (x < placement.getX()) {
			return getCorrectXMove();
		}
		return Command.NONE;
	}

	private int calcMovesYAxis(double y, CustomPoint placement) {
		if (y < placement.getY()) {
			moveX = true;
			return Command.BACKWARDS;
		}
		if (y > placement.getY()) {
			return getCorrectYMove(y);
		}
		return Command.NONE;
	}

}