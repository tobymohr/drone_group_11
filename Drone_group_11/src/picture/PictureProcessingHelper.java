package picture;

import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.CV_8U;
import static org.bytedeco.javacpp.opencv_core.CV_8UC1;
import static org.bytedeco.javacpp.opencv_core.addWeighted;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvPointFrom32f;
import static org.bytedeco.javacpp.opencv_core.inRange;
import static org.bytedeco.javacpp.opencv_imgproc.CV_AA;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2HSV;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_NONE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FILLED;
import static org.bytedeco.javacpp.opencv_imgproc.CV_HOUGH_GRADIENT;
import static org.bytedeco.javacpp.opencv_imgproc.CV_LINK_RUNS;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_EXTERNAL;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2HSV;
import static org.bytedeco.javacpp.opencv_imgproc.Canny;
import static org.bytedeco.javacpp.opencv_imgproc.MORPH_RECT;
import static org.bytedeco.javacpp.opencv_imgproc.RETR_LIST;
import static org.bytedeco.javacpp.opencv_imgproc.approxPolyDP;
import static org.bytedeco.javacpp.opencv_imgproc.arcLength;
import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvHoughCircles;
import static org.bytedeco.javacpp.opencv_imgproc.cvSmooth;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.dilate;
import static org.bytedeco.javacpp.opencv_imgproc.drawContours;
import static org.bytedeco.javacpp.opencv_imgproc.erode;
import static org.bytedeco.javacpp.opencv_imgproc.findContours;
import static org.bytedeco.javacpp.opencv_imgproc.getPerspectiveTransform;
import static org.bytedeco.javacpp.opencv_imgproc.getStructuringElement;
import static org.bytedeco.javacpp.opencv_imgproc.line;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;
import static org.bytedeco.javacpp.opencv_imgproc.moments;
import static org.bytedeco.javacpp.opencv_imgproc.putText;
import static org.bytedeco.javacpp.opencv_imgproc.warpPerspective;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import org.bytedeco.javacpp.opencv_core.CvPoint3D32f;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Moments;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.indexer.IntBufferIndexer;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import coordinateSystem.Vector;
import flightcontrol.ScanSequence;
import helper.Circle;
import helper.CustomPoint;

public class PictureProcessingHelper {

	private static final int MAX_CONTOUR_AREA = 450;
	private OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
	private double distance = 500;
	private Java2DFrameConverter converter1 = new Java2DFrameConverter();
	int blueMin = 110;
	int blueMax = 130;
	public String code = "";
	private int xleft, xright, ytop, ybot, yCenterTop, yCenterBottom;
	private QRCodeReader reader = new QRCodeReader();
	private LuminanceSource source;
	private BinaryBitmap bitmap;
	private Point2f vertices;
	private static final int MIN_AREA = 4000;
	private static final int ANGLE_UPPER_BOUND = 105;
	private static final int ANGLE_LOWER_BOUND = 75;

	public PictureProcessingHelper() {
	}

	double angle(CvPoint pt1, CvPoint pt2, CvPoint pt0) {
		double dx1 = pt1.x() - pt0.x();
		double dy1 = pt1.y() - pt0.y();
		double dx2 = pt2.x() - pt0.x();
		double dy2 = pt2.y() - pt0.y();

		return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
	}

	public Mat findContoursBlueMat(Mat img) {
		Mat mathsv3 = new Mat(img.arraySize(), img.arrayDepth(), img.arrayChannels());
		cvtColor(img, mathsv3, CV_BGR2HSV);
		Mat scalarBlue1 = new Mat(new Scalar(blueMin, 50, 50, 0));
		Mat scalarBlue2 = new Mat(new Scalar(blueMax, 255, 255, 0));
		inRange(mathsv3, scalarBlue1, scalarBlue2, mathsv3);
		return mathsv3;
	}

	public Mat findContoursBlackMat(Mat img) {
		Mat matHSV = new Mat(img.arraySize(), img.arrayDepth(), img.arrayChannels());
		cvtColor(img, matHSV, CV_RGB2HSV);
		Mat scalar1 = new Mat(new Scalar(0, 0, 0, 0));
		Mat scalar2 = new Mat(new Scalar(180, 255, 38, 0));

		inRange(matHSV, scalar1, scalar2, matHSV);
		return matHSV;
	}

	public Mat findContoursRedMat(Mat img) {
		Mat mathsv3 = new Mat(img.arraySize(), CV_8U, 3);
		Mat mathueLower = new Mat(img.arraySize(), CV_8U, 1);
		Mat mathueUpper = new Mat(img.arraySize(), CV_8U, 1);
		Mat imgbin3 = new Mat(img.arraySize(), CV_8U, 3);
		cvtColor(img, mathsv3, CV_BGR2HSV);
		Mat scalar1 = new Mat(new Scalar(0, 180, 60, 0));
		Mat scalar2 = new Mat(new Scalar(23, 255, 255, 0));
		Mat scalar3 = new Mat(new Scalar(158, 180, 60, 0));
		Mat scalar4 = new Mat(new Scalar(180, 255, 255, 0));
		// Two ranges to get full color spectrum
		inRange(mathsv3, scalar1, scalar2, mathueLower);
		inRange(mathsv3, scalar3, scalar4, mathueUpper);
		addWeighted(mathueLower, 1.0, mathueUpper, 1.0, 0.0, imgbin3);
		return imgbin3;
	}

	public Mat findContoursGreenMat(Mat img) {
		Mat imghsv = new Mat(img.arraySize(), 8, 3);
		Mat imgbin = new Mat(img.arraySize(), 8, 1);
		cvtColor(img, imghsv, CV_BGR2HSV);
		Mat scalar1 = new Mat(new Scalar(48, 125, 0, 0));
		Mat scalar2 = new Mat(new Scalar(83, 255, 100, 0));
		// Two ranges to get full color spectrum
		inRange(imghsv, scalar1, scalar2, imgbin);
		return imgbin;
	}

	public ArrayList<CustomPoint> findObjectsMat(Mat filteredImage) {
		ArrayList<CustomPoint> results = null;
		results = new ArrayList<>();

		MatVector contour = new MatVector();
		findContours(filteredImage, contour, RETR_LIST, CV_LINK_RUNS, new opencv_core.Point());
		for (int i = 0; i < contour.size(); i++) {
			if (contourArea(contour.get(i)) > MAX_CONTOUR_AREA) {
				Point2f centerPoint = minAreaRect(contour.get(i)).center();
				opencv_core.Point p = new opencv_core.Point((int) centerPoint.x(), (int) centerPoint.y());

				results.add(new CustomPoint(p.x(), p.y()));
				line(filteredImage, p, p, new Scalar(255, 0, 0, 0), 16, CV_AA, 0);
			}
		}
		return results;
	}

	public ArrayList<CustomPoint> findObjectsMatNew(Mat filteredImage) {
		ArrayList<CustomPoint> results = new ArrayList<>();
		;
		MatVector contours = new MatVector();
		Mat hierarchy = new Mat();
		findContours(filteredImage, contours, hierarchy, RETR_LIST, CV_LINK_RUNS, new opencv_core.Point());
		for (int i = 0; i < contours.size(); i++) {
			// if(contourArea(contour.get(i)) > 50){
			// }
			if (contourArea(contours.get(i)) > MAX_CONTOUR_AREA) {
				Point2f centerPoint = minAreaRect(contours.get(i)).center();
				opencv_core.Point p = new opencv_core.Point((int) centerPoint.x(), (int) centerPoint.y());

				results.add(new CustomPoint(p.x(), p.y()));
				line(filteredImage, p, p, new Scalar(255, 0, 0, 0), 16, CV_AA, 0);
			}
		}
		return results;
	}

	public Mat warpImage(Mat crop, RotatedRect rect) {
		vertices = new Point2f(4);
		rect.points(vertices);
		int angle = Math.abs((int) rect.angle());

		Point tl = null;
		Point tr = null;
		Point br = null;
		Point bl = null;
		float height;
		float width;
		if (angle >= 0 && angle < 10) {
			tl = new Point((int) vertices.position(1).x(), (int) vertices.position(1).y());
			tr = new Point((int) vertices.position(2).x(), (int) vertices.position(2).y());
			br = new Point((int) vertices.position(3).x(), (int) vertices.position(3).y());
			bl = new Point((int) vertices.position(0).x(), (int) vertices.position(0).y());
			height = (int) rect.size().height();
			width = (int) rect.size().width();
		} else {
			tl = new Point((int) vertices.position(2).x(), (int) vertices.position(2).y());
			tr = new Point((int) vertices.position(3).x(), (int) vertices.position(3).y());
			br = new Point((int) vertices.position(0).x(), (int) vertices.position(0).y());
			bl = new Point((int) vertices.position(1).x(), (int) vertices.position(1).y());
			height = rect.size().width();
			width = rect.size().height();
		}

		Point2f source = new Point2f(4);
		Point2f destination = new Point2f(4);

		source.position(0).x(tl.x()).y(tl.y());
		source.position(1).x(tr.x()).y(tr.y());
		source.position(2).x(bl.x()).y(bl.y());
		source.position(3).x(br.x()).y(br.y());

		destination.position(0).x(0.0f).y(0.0f);
		destination.position(1).x(width).y(0.0f);
		destination.position(2).x(0.0f).y(height);
		destination.position(3).x(width).y(height);

		Mat homograpyMat = new Mat(3, 3, CV_32FC1);
		Mat copyMat = crop.clone();

		homograpyMat = getPerspectiveTransform(source.position(0), destination.position(0));
		warpPerspective(copyMat, copyMat, homograpyMat, copyMat.size());
		copyMat.adjustROI(0, 0, (int) width, (int) height);
		Mat result = new Mat(copyMat, new Rect(0, 0, (int) width, (int) height));
		return result;
	}

	public double isCenterInImage(Mat img, RotatedRect rect) {
		double factor = 3.5;
		double xleft = img.arrayWidth() / factor;
		double xright = (img.arrayWidth() / factor) * (factor - 1);
		double middleX = img.arrayWidth() / 2;
		return checkForCenterInImage(rect.center().x(), xleft, xright, middleX);
	}

	public double isCenterInImageBigger(Mat img, RotatedRect rect) {
		double factor = 3.5;
		double xleft = img.arrayWidth() / factor;
		double xright = (img.arrayWidth() / factor) * (factor - 1);
		double middleX = img.arrayWidth() / 2;
		return checkForCenterInImage(rect.center().x(), xleft, xright, middleX);
	}

	private double checkForCenterInImage(float posX, double xLeft, double xRight, double middleX) {

		boolean leftCenterCondition = posX > xLeft;
		boolean rightCenterCondition = posX < xRight;

		if (leftCenterCondition && rightCenterCondition) {
			return 0;
		} else if (posX < xLeft) {
			return posX - middleX;
		} else if (posX > xRight) {
			return posX - middleX;
		}
		return 0;

	}

	public int getSpinSpeed(double area) {
		double constant = 500;
		double result = area / constant;
		if (!Double.isNaN(result)) {
			return (int) result;
		}
		return 0;
	}

	private RotatedRect mostCenteredRect(List<Mat> contours, Mat srcImage) {
		double distanceFomCenter = Double.MAX_VALUE;
		RotatedRect rect = new RotatedRect();
		for (int i = 0; i < contours.size(); i++) {
			if (contourArea(contours.get(i)) > 1000) {
				RotatedRect rect2 = minAreaRect(contours.get(i));
				int angle = Math.abs((int) rect.angle());
				float height;
				float width;
				if (angle >= 0 && angle < 10) {
					height = (int) rect2.size().height();
					width = (int) rect2.size().width();
				} else {
					height = rect2.size().width();
					width = rect2.size().height();
				}
				double distance = (srcImage.arrayWidth() / 2) - rect2.center().x();
				double ratio = height / width;
				if (distanceFomCenter > distance && ratio > 1.15) {
					distanceFomCenter = Math.abs(distance);
					rect = rect2;
				}
			}
		}
		return rect;
	}

	public Mat extractQRImage(Mat srcImage) {
		Mat img1 = new Mat(srcImage.arraySize(), CV_8UC1, 1);
		List<Mat> matsForRects = new ArrayList<>();
		cvtColor(srcImage, img1, CV_RGB2GRAY);
		Canny(img1, img1, 75, 200);
		MatVector matContour = new MatVector();
		findContours(img1, matContour, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);

		for (int i = 0; i < matContour.size(); i++) {
			approxPolyDP(matContour.get(i), matContour.get(i), 0.02 * arcLength(matContour.get(i), true), true);
			RotatedRect rect = minAreaRect(matContour.get(i));

			if (matContour.get(i).total() == 4 && contourArea(matContour.get(i)) > MIN_AREA
					&& checkAngles(matContour.get(i), rect)) {
				matsForRects.add(matContour.get(i));

				drawContours(srcImage, matContour, i, Scalar.WHITE, 3, 8, null, 1, null);
				img1 = warpImage(srcImage, rect);
				String qrCode = scanQrCode(img1);
				if (qrCode != null && !qrCode.equals("")) {
					putText(srcImage, qrCode, new Point((int) rect.center().x() - 25, (int) rect.center().y() + 80), 1,
							2, Scalar.GREEN, 2, 8, false);
				}
				distance = calcDistance(rect);
				putText(srcImage, "" + distance, new Point((int) rect.center().x() - 25, (int) rect.center().y() + 60),
						1, 2, Scalar.BLUE, 2, 8, false);

				putText(srcImage, "" + contourArea(matContour.get(i)),
						new Point((int) rect.center().x() - 25, (int) rect.center().y() + 150), 1, 2, Scalar.BLACK, 2,
						8, false);
				double center = center(rect);
				if (center < ScanSequence.CENTER_UPPER && center > ScanSequence.CENTER_LOWER
						&& isCenterInImage(srcImage, rect) == 0) {
					putText(srcImage, "CENTER", new Point((int) rect.center().x() - 25, (int) rect.center().y() + 20),
							1, 2, Scalar.RED, 2, 8, false);
				}
			}
		}

		return srcImage;
	}

	public List<Mat> findQrContours(Mat srcImage) {
		Mat img1 = new Mat(srcImage.arraySize(), CV_8UC1, 1);
		cvtColor(srcImage, img1, CV_RGB2GRAY);
		Canny(img1, img1, 75, 200);
		MatVector matContour = new MatVector();
		List<Mat> result = new ArrayList<>();
		findContours(img1, matContour, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);

		for (int i = 0; i < matContour.size(); i++) {
			approxPolyDP(matContour.get(i), matContour.get(i), 0.02 * arcLength(matContour.get(i), true), true);
			RotatedRect rect = minAreaRect(matContour.get(i));
			if (matContour.get(i).total() == 4 && contourArea(matContour.get(i)) > MIN_AREA
					&& checkAngles(matContour.get(i), rect)) {
				result.add(matContour.get(i));
			}
		}
		return result;
	}

	public List<Mat> findQrContoursNoThresh(Mat srcImage) {
		// Mat img1 = new Mat(srcImage.arraySize(), CV_8UC1, 1);
		Mat img1 = findContoursBlackMat(srcImage);
		// cvtColor(srcImage, img1, CV_RGB2GRAY);
		// Canny(img1, img1, 75, 200);
		MatVector matContour = new MatVector();
		List<Mat> result = new ArrayList<>();
		findContours(img1, matContour, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);
		for (int i = 0; i < matContour.size(); i++) {
			approxPolyDP(matContour.get(i), matContour.get(i), 0.02 * arcLength(matContour.get(i), true), true);
			RotatedRect rect = minAreaRect(matContour.get(i));
			if (matContour.get(i).total() == 4 && contourArea(matContour.get(i)) > MIN_AREA
					&& checkAngles(matContour.get(i), rect)) {
				result.add(matContour.get(i));
			}
		}
		return result;
	}

	public CustomPoint[] calcPosition(double distanceOne, double distanceTwo, double distanceThree, String code) {
		CustomPoint P1 = CustomPoint.parseQRTextLeft(code);
		CustomPoint P2 = CustomPoint.parseQRText(code);
		CustomPoint P3 = CustomPoint.parseQRTextRight(code);

		double distanceBetweenPointsOne = CustomPoint.calculateDistance(P1, P2);
		double distanceBetweenPointsTwo = CustomPoint.calculateDistance(P2, P3);

		double angleA = CustomPoint.calculateAngle(distanceOne, distanceBetweenPointsOne);
		double angleB = CustomPoint.calculateAngle(distanceThree, distanceBetweenPointsTwo);

		Circle C1 = new Circle();
		C1.setCenter(Circle.calculateCenter(P1, P2, distanceBetweenPointsOne, angleA));
		C1.setRadius(Circle.calculateRadius(distanceBetweenPointsOne, angleA));

		Circle C2 = new Circle();
		C2.setCenter(Circle.calculateCenter(P2, P3, distanceBetweenPointsTwo, angleB));
		C2.setRadius(Circle.calculateRadius(distanceBetweenPointsTwo, angleB));

		CustomPoint[] points = Circle.intersection(C1, C2);
		return points;
	}

	public String scanQrCode(Mat srcImage) {
		BufferedImage qrCode = converter1.convert(converter.convert(srcImage));
		source = new BufferedImageLuminanceSource(qrCode);
		bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			Result detectionResult = reader.decode(bitmap);
			String code = detectionResult.getText();
			return code;
		} catch (Exception e) {
			return "";
		}

	}

	public boolean checkAngles(Mat contour, RotatedRect rect) {
		List<Point> points = new ArrayList<>();
		IntBufferIndexer idx = contour.createIndexer();
		for (int j = 0; j < contour.rows(); j++) {
			for (int k = 0; k < contour.cols(); k++) {
				int x = (int) idx.get(j, k, 0);
				int y = idx.get(j, k, 1);
				points.add(new Point(x, y));
			}
		}

		vertices = new Point2f(4);
		rect.points(vertices);
		int angle = Math.abs((int) rect.angle());

		Point tlRect = null;
		Point trRect = null;
		Point brRect = null;
		Point blRect = null;
		if (angle >= 0 && angle < 10) {
			tlRect = new Point((int) vertices.position(1).x(), (int) vertices.position(1).y());
			trRect = new Point((int) vertices.position(2).x(), (int) vertices.position(2).y());
			brRect = new Point((int) vertices.position(3).x(), (int) vertices.position(3).y());
			blRect = new Point((int) vertices.position(0).x(), (int) vertices.position(0).y());
		} else {
			tlRect = new Point((int) vertices.position(2).x(), (int) vertices.position(2).y());
			trRect = new Point((int) vertices.position(3).x(), (int) vertices.position(3).y());
			brRect = new Point((int) vertices.position(0).x(), (int) vertices.position(0).y());
			blRect = new Point((int) vertices.position(1).x(), (int) vertices.position(1).y());
		}

		Point tl = nearestPoint(points, tlRect);
		Point tr = nearestPoint(points, trRect);
		Point br = nearestPoint(points, brRect);
		Point bl = nearestPoint(points, blRect);

		double tlAngle = calculateAngle(tl, tr, bl);
		double trAngle = calculateAngle(tr, tl, br);
		double blAngle = calculateAngle(bl, tl, br);
		double brAngle = calculateAngle(br, tr, bl);

		if (tlAngle > ANGLE_UPPER_BOUND || tlAngle < ANGLE_LOWER_BOUND || Double.isNaN(tlAngle)) {
			return false;
		}
		if (trAngle > ANGLE_UPPER_BOUND || tlAngle < ANGLE_LOWER_BOUND || Double.isNaN(trAngle)) {
			return false;
		}
		if (blAngle > ANGLE_UPPER_BOUND || tlAngle < ANGLE_LOWER_BOUND || Double.isNaN(blAngle)) {
			return false;
		}
		if (brAngle > ANGLE_UPPER_BOUND || brAngle < ANGLE_LOWER_BOUND || Double.isNaN(brAngle)) {
			return false;
		} else {
			return true;
		}
	}

	private Point nearestPoint(List<Point> points, Point point) {
		double minDist = Double.MAX_VALUE;
		Point result = new Point();
		for (Point p : points) {
			double dist = Math.sqrt(Math.pow((point.x() - p.x()), 2) + Math.pow((point.y() - p.y()), 2));
			if (dist < minDist) {
				minDist = dist;
				result = p;
			}
		}
		return result;
	}

	private double calculateAngle(Point A, Point B, Point C) {
		double a = Math.sqrt(Math.pow(C.x() - B.x(), 2) + Math.pow(C.y() - B.y(), 2));
		double b = Math.sqrt(Math.pow(A.x() - C.x(), 2) + Math.pow(A.y() - C.y(), 2));
		double c = Math.sqrt(Math.pow(B.x() - A.x(), 2) + Math.pow(B.y() - A.y(), 2));
		double cosA = (Math.pow(b, 2) + Math.pow(c, 2) - Math.pow(a, 2)) / (2 * b * c);
		return Math.toDegrees(Math.acos(cosA));
	}

	public boolean checkDecodedQR(Mat img) {
		String OURQR = "AF.04";

		BufferedImage qrCode = converter1.convert(converter.convert(img));
		source = new BufferedImageLuminanceSource(qrCode);
		bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			Result detectionResult = reader.decode(bitmap);
			code = detectionResult.getText();
			if (code.equals(OURQR)) {
				return true;
			}

		} catch (Exception e) {
			return false;
		}

		return false;
	}

	public double calcDistance(RotatedRect rect) {
		double knownDistance = 247.5;
		double height = 0;
		double focalLength = 181 * knownDistance;
		int angle = Math.abs((int) rect.angle());
		if (angle >= 0 && angle < 10) {
			height = rect.size().height();
		} else {
			height = rect.size().width();
		}
		return focalLength / height;
	}

	public Mat center(Mat img, Mat filter) {

		MatVector matContour = new MatVector();

		Mat img1 = new Mat(img.arraySize(), CV_8UC1, 1);

		cvtColor(img, img1, CV_RGB2GRAY);

		Canny(img1, img1, 75, 200);

		findContours(img1, matContour, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);

		int factor = 3;

		// find center points
		xleft = (int) img.arrayWidth() / factor;
		xright = (int) (img.arrayWidth() / factor) * (factor - 1);
		ytop = 0;
		ybot = img.arrayHeight();
		// center of centerpoints y
		yCenterBottom = (img.arrayHeight() / 3) * 2;
		yCenterTop = (img.arrayHeight() / 3);

		// // center points
		// int xcenter = img.arrayWidth()/2;
		// int ycenter = img.arrayHeight()/2;

		// Make center points
		Point pointTopLeft = new Point(xleft, ytop);
		Point pointBottomLeft = new Point(xleft, ybot);
		Point pointTopRight = new Point(xright, ytop);
		Point pointRightBottom = new Point(xright, ybot);

		// Make upper line points in center
		Point pointCenterUpperLeft = new Point(xleft, yCenterTop);
		Point pointCenterUpperRight = new Point(xright, yCenterTop);
		// Make bottom line points in center
		Point pointCenterBottomLeft = new Point(xleft, yCenterBottom);
		Point pointCenterBottomRight = new Point(xright, yCenterBottom);

		// Find red point
		int posX = 0;
		int posY = 0;

		Moments moments = moments(filter);
		double mom10 = moments.m10();
		double mom01 = moments.m01();
		double mom00 = moments.m00();
		posX = (int) (mom10 / mom00);
		posY = (int) (mom01 / mom00);

		int counter = 0;
		for (int i = 0; i < matContour.size(); i++) {

			approxPolyDP(matContour.get(i), matContour.get(i), 0.02 * arcLength(matContour.get(i), true), true);
			if (matContour.get(i).total() == 4 && contourArea(matContour.get(i)) > 1000
					&& contourArea(matContour.get(i)) < 10000) {
				Point2f centerPoint = minAreaRect(matContour.get(i)).center();
				opencv_core.Point p = new opencv_core.Point((int) centerPoint.x(), (int) centerPoint.y());
				line(img, p, p, Scalar.BLACK, 16, CV_AA, 0);
				drawContours(img1, matContour, i, Scalar.WHITE, CV_FILLED, 8, null, 1, null);
				for (int j = 0; j < matContour.get(i).total(); j++) {
					Point2f centerPointTemp = minAreaRect(matContour.get(i)).center();
					opencv_core.Point ptemp = new opencv_core.Point((int) centerPointTemp.x(),
							(int) centerPointTemp.y());
					line(img, ptemp, ptemp, Scalar.BLACK, 16, CV_AA, 0);
					if (checkBoxForCenter(ptemp.x(), ptemp.y())) {
						counter++;
					}
				}

				if (counter == matContour.get(i).total()) {
					// check in which part of center box is.

					switch (checkPositionInCenter(p.x(), p.y())) {
					case 1:
						line(img, p, p, Scalar.BLUE, 16, CV_AA, 0);
						break;
					case 2:
						line(img, p, p, Scalar.RED, 16, CV_AA, 0);
						break;
					case 3:
						line(img, p, p, Scalar.GREEN, 16, CV_AA, 0);
						break;
					case 4:
						line(img, p, p, Scalar.BLACK, 16, CV_AA, 0);
					case 5:
						line(img, p, p, Scalar.BLACK, 16, CV_AA, 0);
					default:
						break;

					}

				}
			}
		}
		line(img, pointTopLeft, pointTopRight, new Scalar(255, 0, 255, 0));
		line(img, pointTopRight, pointRightBottom, new Scalar(255, 0, 255, 0));
		line(img, pointRightBottom, pointBottomLeft, new Scalar(255, 0, 255, 0));
		line(img, pointBottomLeft, pointTopLeft, new Scalar(255, 0, 255, 0));
		// Draw upper line
		line(img, pointCenterUpperLeft, pointCenterUpperRight, new Scalar(0, 0, 255, 0));
		line(img, pointCenterBottomLeft, pointCenterBottomRight, new Scalar(0, 255, 0, 0));

		return img;
	}

	public IplImage convertMatToIplImage(Mat mat) {
		return converter.convert(converter.convert(mat));
	}

	public synchronized Mat findPolygonsMat(Mat coloredImage, Mat filteredImage, int edgeNumber) {

		MatVector contour = new MatVector();
		findContours(filteredImage, contour, RETR_LIST, CV_LINK_RUNS, new opencv_core.Point());

		for (int i = 0; i < contour.size(); i++) {
			approxPolyDP(contour.get(i), contour.get(i), 0.02 * arcLength(contour.get(i), true), true);
			if (contour.get(i).total() > 2 && contour.get(i).total() < 6 && contourArea(contour.get(i)) > 150) {
				Point2f centerPoint = minAreaRect(contour.get(i)).center();
				opencv_core.Point p = new opencv_core.Point((int) centerPoint.x(), (int) centerPoint.y());
				line(coloredImage, p, p, new Scalar(255, 0, 0, 0), 16, CV_AA, 0);
				drawContours(coloredImage, contour, i, new Scalar(0, 0, 0, 0), 3, CV_AA, null, 1,
						new opencv_core.Point());
			}
		}

		return coloredImage;
	}

	public String getQrCode() {
		return code;
	}

	public double getDistance() {
		return distance;
	}

	private int checkPositionInCenter(int posx, int posy) {

		boolean bottomCenterCondition = posy > yCenterBottom;
		boolean upperCenterCondition = posy < yCenterTop;
		boolean leftCenterCondition = posx < xleft;
		boolean rightCenterCondition = posx > xright;

		if (upperCenterCondition) {

			return 1;
		}

		if (!bottomCenterCondition && !upperCenterCondition) {

			return 2;
		}

		if (bottomCenterCondition) {

			return 3;
		}
		if (leftCenterCondition) {

			return 4;
		}

		if (rightCenterCondition) {
			return 5;
		}
		return 0;

	}

	private boolean checkBoxForCenter(int posx, int posy) {

		boolean verticalCondition = posy > ytop && posy < ybot;
		boolean horizontalCondition = posx > xleft && posx < xright;
		if (horizontalCondition && verticalCondition) {

			return true;
		} else {
			return false;
		}

	}

	private boolean checkForCenter(int posx, int posy, int redx, int redy) {
		boolean redverticalCondition = redy > ytop && redy < ybot;
		boolean redhorizontalCondition = redx > xleft && redx < xright;

		boolean verticalCondition = posy > ytop && posy < ybot;
		boolean horizontalCondition = posx > xleft && posx < xright;
		if (horizontalCondition && verticalCondition && redverticalCondition && redhorizontalCondition) {
			return true;
		} else {
			return false;
		}

	}

	public Vector convertToVector(String point) {
		int firstIndex = point.toString().lastIndexOf(',');
		int xcord = Integer.parseInt(point.toString().substring(0, firstIndex).replaceAll("[^0-9]", ""));
		int ycord = Integer
				.parseInt(point.toString().substring(firstIndex, point.toString().length()).replaceAll("[^0-9]", ""));
		return new Vector(xcord, ycord);
	}

	public Mat erodeAndDilate(Mat thresh) {
		Mat erodeElement = getStructuringElement(MORPH_RECT, new Size(3, 3));
		Mat dilateElement = getStructuringElement(MORPH_RECT, new Size(8, 8));
		erode(thresh, thresh, erodeElement);
		erode(thresh, thresh, erodeElement);
		dilate(thresh, thresh, dilateElement);
		dilate(thresh, thresh, dilateElement);
		return thresh;
	}

	public double center(RotatedRect rect) {
		float height;
		float width;
		if (rect.angle() >= 0 && rect.angle() < 10) {
			height = rect.size().height();
			width = rect.size().width();
		} else {
			height = rect.size().width();
			width = rect.size().height();
		}

		float ratio = height / width;
		return ratio - 1.43;
	}

	public int findCircles(Mat img) {

		IplImage src = new IplImage(img);
		IplImage gray = cvCreateImage(cvGetSize(src), 8, 1);

		cvCvtColor(src, gray, CV_BGR2GRAY);
		cvSmooth(gray, gray);
		CvMemStorage mem = CvMemStorage.create();

		CvSeq circles = cvHoughCircles(gray, // Input image
				mem, // Memory Storage
				CV_HOUGH_GRADIENT, // Detection method
				1, // Inverse ratio
				100, // Minimum distance between the centers of the detected
						// circles
				60, // Higher threshold for canny edge detector
				100, // Threshold at the center detection stage
				15, // min radius
				700 // max radius
		);

		for (int i = 0; i < circles.total(); i++) {
			CvPoint3D32f circle = new CvPoint3D32f(cvGetSeqElem(circles, i));
			CvPoint center = cvPointFrom32f(new CvPoint2D32f(circle.x(), circle.y()));
			int radius = Math.round(circle.z());
//			circle(img, new Point(center.x(), center.y()), radius, new Scalar(20,255,20,0), 5, CV_AA, 0);
			
		}

		return circles.total();
	}

	public boolean isBlueTowerAhead(MatVector contours) {
		return contours.size() > 300 ? true : false;
	}
}
