package picture;

import static org.bytedeco.javacpp.helper.opencv_core.*;


import picture.PictureController;
import static org.bytedeco.javacpp.helper.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_video.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import org.bytedeco.javacv.*;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.indexer.FloatIndexer;

import static org.bytedeco.javacpp.opencv_core.*;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;

import helper.Circle;
import helper.Point;
import helper.Vector;

public class PictureProcessingHelper {

	private static final int MAX_CORNERS = 200;
	OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
	Java2DFrameConverter converter1 = new Java2DFrameConverter();
	private CvMemStorage storage = CvMemStorage.create();
	static int maxRed = 242;
	static int maxGreen = 99;
	static int maxBlue = 255;
	static int minRed = 0;
	static int minGreen = 0;
	static int minBlue = 134;
	static int smoother = 11;
	private int minThresh = 30;
	private int i = 0;
	CvPoint2D32f c1 = new CvPoint2D32f(4);
	CvPoint2D32f c2 = new CvPoint2D32f(4);

	private CvScalar rgba_min = cvScalar(minRed, minGreen, minBlue, 0);
	private CvScalar rgba_max = cvScalar(maxRed, maxGreen, maxBlue, 0);
	private int xleft, xright, ytop, ybot, yCenterTop, yCenterBottom;
	QRCodeReader reader = new QRCodeReader();
	LuminanceSource source;
	BinaryBitmap bitmap;
	List<CvPoint> corners = new ArrayList<CvPoint>();
	IplImage mask;
	IplImage crop;
	IplImage imgWarped;
	IplImage imgSharpened;

	CvSeq squares = cvCreateSeq(0, Loader.sizeof(CvSeq.class), Loader.sizeof(CvPoint.class), storage);
    CanvasFrame canvas = new CanvasFrame("Warped Image");
    CanvasFrame canvas1 = new CanvasFrame("Sharpened Image");
	private CvBox2D markerRight;
	private CvBox2D markerLeft;
	private CvPoint pointMiddle;
	private CvPoint pointClosest;
	private CvBox2D markerMiddle;
	private IplImage imghsv;
	private IplImage imgbin;
	private CvScalar bminc;
	private CvScalar bmaxc;
	private CvScalar rminc;
	private CvScalar rmaxc;
	private CvSeq contour1;
	private CvSeq contour2;
	private CvMemStorage storage2;
	private Mat kernel;
	private FloatIndexer ki;
	private Mat dest;
	private CvMat homography;
	private IplImage img1;
	private CvSeq contour;
	private IplImage crop2;
	private IplImage mask2;
	private IplImage imghsv2;
	private IplImage imgbin2;
	private IplImage grayImage;
	private IplImage hueLower;
	private IplImage hueUpper;
	private IplImage imghsv3;
	private IplImage imgbin3;
	private IplImage imghsv4;
	private IplImage imgbin4;
	private IplImage imgB;
	private IplImage imgC;
	private IplImage eig_image;
	private IplImage tmp_image;
	private IplImage pyrA;
	private IplImage pyrB;
	
	
	public void releaseAll(){
		cvReleaseImage(img1);
		cvReleaseImage(crop2);
		cvReleaseImage(mask2);
		cvReleaseImage(imghsv2);
		cvReleaseImage(imgbin2);
		cvReleaseImage(grayImage);
		cvReleaseImage(hueLower);
		cvReleaseImage(hueUpper);
		cvReleaseImage(imghsv3);
		cvReleaseImage(imgbin3);
		cvReleaseImage(imghsv4);
		cvReleaseImage(imgbin4);
		cvReleaseImage(imgB);
		cvReleaseImage(imgC);
		cvReleaseImage(eig_image);
		cvReleaseImage(tmp_image);
		cvReleaseImage(pyrA);
		cvReleaseImage(pyrB);
		
		
	}

	public PictureProcessingHelper() {
	    canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    canvas1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    canvas.addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
				System.out.println(canvas.getWidth()+ " "+  canvas.getHeight());
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
	}


	double angle(CvPoint pt1, CvPoint pt2, CvPoint pt0) {
		double dx1 = pt1.x() - pt0.x();
		double dy1 = pt1.y() - pt0.y();
		double dx2 = pt2.x() - pt0.x();
		double dy2 = pt2.y() - pt0.y();

		return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
	}

	public IplImage findMoments(IplImage img) {

		bminc = cvScalar(95, 150, 75, 0);
		bmaxc = cvScalar(145, 255, 255, 0);
		rminc = cvScalar(150, 150, 75, 0);
		rmaxc = cvScalar(190, 255, 255, 0);
		contour1 = new CvSeq();
		storage2 = CvMemStorage.create();
		double areaMax, areaC = 0;

		return null;
	}
	public IplImage sharpenImage(IplImage img0){
		kernel = new Mat(3, 3, CV_32F, new Scalar(0));
		  ki = kernel.createIndexer();
		  ki.put(1, 1, 5);
		  ki.put(0, 1, -1);
		  ki.put(2, 1, -1);
		  ki.put(1, 0, -1);
		  ki.put(1, 2, -1);
dest = new Mat();
		filter2D(cvarrToMat(img0), dest, img0.depth(), kernel);

		imgSharpened = new IplImage(dest);
		return imgSharpened;
	}

	public IplImage warpImage(IplImage crop, CvSeq points) {
		canvas1.showImage(converter.convert(crop));
		crop = sharpenImage(crop);
		corners.clear();
		for (int i = 0; i < 4; i++) {
			CvPoint p = new CvPoint(cvGetSeqElem(points, i));
			corners.add(p);
		}
		
		float[] aImg = { 
				corners.get(0).x(), corners.get(0).y(), 
				corners.get(1).x(), corners.get(1).y(), 
				corners.get(2).x(), corners.get(2).y(), 
				corners.get(3).x(), corners.get(3).y()
		};
		
		int qrHeight = corners.get(1).y() - corners.get(0).y();
		int qrWidth = corners.get(3).x() - corners.get(0).x();
		if (qrHeight <= 0 || qrWidth <= 0 || ((int)qrHeight/qrWidth) == 0) {
			return crop;
		}
		float aspect = qrHeight / qrWidth;
		int height = 146;
		int width = 98;
//		System.out.println("Aspect " + aspect + " width " + width + " height " + height );
		float[] aWorld = { 
				0.0f, 			0.0f,
				0.0f, 			height*4,
				width*4, 			height*4,
				width*4,		 	0.0f 
				};

		homography = cvCreateMat(3,3, opencv_core.CV_32FC1);
		opencv_imgproc.cvGetPerspectiveTransform(aImg, aWorld, homography);

		imgWarped = cvCreateImage(new CvSize(width*4, height*4), 8, 3);
		cvResize(imgWarped, imgWarped, 1/4);
		cvWarpPerspective(crop, imgWarped, homography, opencv_imgproc.CV_INTER_LINEAR, CvScalar.ZERO);
		cvSmooth(imgWarped, imgWarped, 2, 21, 0, 0, 0);
		canvas.showImage(converter.convert(imgWarped));
		return imgWarped;
	}
	
	public void transformForDistance() {
	}

	public IplImage extractQRImage(IplImage img0) {
		cvClearMemStorage(storage);
		float known_distance = 200;
		float known_width = 28;
		float focalLength = (113 * known_distance) / known_width;
		float distance_between_points = 150;

		img1 = cvCreateImage(cvGetSize(img0), IPL_DEPTH_8U, 1);
		cvCvtColor(img0, img1, CV_RGB2GRAY);

		cvCanny(img1, img1, 100, 200);
		contour = new CvSeq(null);
		cvFindContours(img1, storage, contour, Loader.sizeof(CvContour.class), CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);

		List<CvBox2D> markers = new ArrayList<>();
		List<CvSeq> pointsList = new ArrayList<>();
		String code = "";
		int foundIndex = 0;
		
		
		crop2 = cvCreateImage(cvGetSize(img1), IPL_DEPTH_8U, img0.nChannels());
		mask2 = cvCreateImage(cvGetSize(img1), IPL_DEPTH_8U, img0.nChannels());
		cvSetZero(crop2);
		cvSetZero(mask2);
		boolean found = false;
		BufferedImage qrCode;

		while (contour != null && !contour.isNull()) {
			if (contour.elem_size() > 0) {
				CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
						cvContourPerimeter(contour) * 0.02, 0);
				if (points.total() == 4 && cvContourArea(points) > 150 && cvContourArea(points) < 10000) {
					mask = cvCreateImage(cvGetSize(img1), IPL_DEPTH_8U, img1.nChannels());
					crop = cvCreateImage(cvGetSize(img1), IPL_DEPTH_8U, img0.nChannels());
					cvSetZero(crop);
					cvSetZero(mask);
					cvDrawContours(mask, points, CvScalar.WHITE, CV_RGB(248, 18, 18), 1, -1, 8);
					cvCopy(img0, crop, mask);
					cvDrawContours(mask2, points, CvScalar.WHITE, CV_RGB(248, 18, 18), 1, -1, 8);
					cvCopy(img0, crop2, mask2);
					
					// Draw red point
					pointsList.add(points);
					
					crop = warpImage(crop, points);
					qrCode = converter1.convert(converter.convert(crop));
					source = new BufferedImageLuminanceSource(qrCode);
					bitmap = new BinaryBitmap(new HybridBinarizer(source));
					try {
						Result detectionResult = reader.decode(bitmap);
						code = detectionResult.getText();
						found = true;
					} catch (NotFoundException e) {
//						e.printStackTrace();
					} catch (ChecksumException e) {
//						e.printStackTrace();
					} catch (FormatException e) {
//						e.printStackTrace();
					}
					if (!found)foundIndex++;
				}
			}
			contour = contour.h_next();
		}
		if (found && pointsList.size() >= 3) {
			markerRight = new CvBox2D();
			markerLeft = new CvBox2D();
			
			CvSeq pointsMiddle = pointsList.get(foundIndex);
			markerMiddle = cvMinAreaRect2(pointsMiddle, storage);
			pointMiddle = new CvPoint(cvGetSeqElem(pointsList.get(foundIndex), 0));
			pointsList.remove(foundIndex);
			
			int indexOne = closestPoint(pointsList, pointsMiddle);
			CvSeq pointsClosest = pointsList.get(indexOne);
			pointClosest = new CvPoint(cvGetSeqElem(pointsClosest, 0));
			if (pointClosest.x() < pointMiddle.x()) {
				markerLeft = cvMinAreaRect2(pointsClosest, storage);
			} else {
				markerRight = cvMinAreaRect2(pointsClosest, storage);
			}
			pointsList.remove(indexOne);
			
			indexOne = closestPoint(pointsList, pointsMiddle);
			pointsClosest = pointsList.get(indexOne);
			pointClosest = new CvPoint(cvGetSeqElem(pointsClosest, 0));
			if (pointClosest.x() < pointMiddle.x()) {
				markerLeft = cvMinAreaRect2(pointsClosest, storage);
			} else {
				markerRight = cvMinAreaRect2(pointsClosest, storage);
			}
			
			double distanceOne = (known_width * focalLength) / markerLeft.get(2);
			double distanceTwo = (known_width * focalLength) / markerMiddle.get(2);
			double distanceThree = (known_width * focalLength) / markerRight.get(2);
			System.out.println("--------------------------------");
			System.out.println(distanceOne + "|" + distanceTwo + "|" + distanceThree);
			double angleA = Point.calculateAngle(distanceOne, distance_between_points);
			double angleB = Point.calculateAngle(distanceThree, distance_between_points);
			Point P1 = Point.parseQRTextLeft(code);
			Point P2 = Point.parseQRText(code);
			Point P3 = Point.parseQRTextRight(code);
			System.out.println("(" + P1.getX() + "," + P1.getY() + ")" + "(" + P2.getX() + "," + P2.getY() + ")" + "(" + P3.getX() + "," + P3.getY() + ")");
			Circle C1 = new Circle(Circle.calculateCenter(P1, P2, distance_between_points, angleA), 
					Circle.calculateRadius(distance_between_points, angleA));
			Circle C2 = new Circle(Circle.calculateCenter(P2, P3, distance_between_points, angleB), 
					Circle.calculateRadius(distance_between_points, angleB));
			Point[] points = Circle.intersection(C1, C2);
			for (Point p : points) {
				System.out.println(Math.round(p.getX()) + "|" + Math.round(p.getY()));
			}			
			System.out.println("--------------------------------");
		}
		return crop2;
	}
	
	private int closestPoint(List<CvSeq> pointsList, CvSeq markerMiddle) {
		double qrMarkerSize = cvContourArea(markerMiddle);
		double distance = Math.abs(cvContourArea(pointsList.get(0)) - qrMarkerSize);
		int index = 0;
		for (int i = 1; i < pointsList.size(); i++) {
			double newDistance = Math.abs(cvContourArea(pointsList.get(i)) - qrMarkerSize);
			if (newDistance < distance) {
				index = i;
				distance = newDistance;
			}
		}
		return index;
	}

	public IplImage findContoursBlue(IplImage img) {

		// Blue
		CvScalar minc = cvScalar(95, 150, 75, 0), maxc = cvScalar(145, 255, 255, 0);

		CvSeq contour1 = new CvSeq(), contour2;
		CvMemStorage storage = CvMemStorage.create();
		double areaMax = 1000, areaC = 0;

		imghsv2 = cvCreateImage(cvGetSize(img), 8, 3);
		imgbin2 = cvCreateImage(cvGetSize(img), 8, 1);

		cvCvtColor(img, imghsv2, CV_BGR2HSV);
		cvInRangeS(imghsv2, minc, maxc, imgbin2);

		cvFindContours(imgbin2, storage, contour1, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS,
				cvPoint(0, 0));
		
		erodeAndDilate(imgbin2);

		contour2 = contour1;

		while (contour1 != null && !contour1.isNull()) {
			areaC = cvContourArea(contour1, CV_WHOLE_SEQ, 1);
			if (areaC > areaMax)
				areaMax = areaC;
			contour1 = contour1.h_next();

		}

		while (contour2 != null && !contour2.isNull()) {
			areaC = cvContourArea(contour2, CV_WHOLE_SEQ, 1);
			if (areaC < areaMax) {
				cvDrawContours(imgbin2, contour2, CV_RGB(0, 0, 0), CV_RGB(0, 0, 0), 0, CV_FILLED, 8, cvPoint(0, 0));
			}
			contour2 = contour2.h_next();
		}

		cvSmooth(imgbin2, imgbin2, 2, smoother, 0, 0, 0);
		return imgbin2;

	}

	public IplImage findContoursBlack(IplImage img) {
		CvSeq contour1 = new CvSeq(), contour2;
		CvMemStorage storage = CvMemStorage.create();
		double areaMax = 1000, areaC = 0;
		grayImage = IplImage.create(img.width(), img.height(), IPL_DEPTH_8U, 1);
		cvCvtColor(img, grayImage, CV_BGR2GRAY);

		cvThreshold(grayImage, grayImage, 100, 255, CV_THRESH_BINARY);

		cvFindContours(grayImage, storage, contour1, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS,
				cvPoint(0, 0));
		

		contour2 = contour1;

		while (contour1 != null && !contour1.isNull()) {
			areaC = cvContourArea(contour1, CV_WHOLE_SEQ, 1);
			if (areaC > areaMax)
				areaMax = areaC;
			contour1 = contour1.h_next();

		}

		while (contour2 != null && !contour2.isNull()) {
			areaC = cvContourArea(contour2, CV_WHOLE_SEQ, 1);
			if (areaC < areaMax) {
				cvDrawContours(grayImage, contour2, CV_RGB(0, 0, 0), CV_RGB(0, 0, 0), 0, CV_FILLED, 8, cvPoint(0, 0));
			}
			contour2 = contour2.h_next();
		}
		// cvSmooth(imgbin, imgbin, 3, smoother, 0, 0, 0);
		return grayImage;
	}

	public IplImage findContoursRed(IplImage img) {

		hueLower = null;
		hueUpper = null;
		// img = balanceWhite(img);
		CvSeq contour1 = new CvSeq(), contour2;
		CvMemStorage storage = CvMemStorage.create();
		double areaMax = 1000, areaC = 0;

		imghsv3 = cvCreateImage(cvGetSize(img), 8, 3);
		imgbin3 = cvCreateImage(cvGetSize(img), 8, 1);
		hueLower = cvCreateImage(cvGetSize(img), 8, 1);
		hueUpper = cvCreateImage(cvGetSize(img), 8, 1);

		cvCvtColor(img, imghsv3, CV_BGR2HSV);
		
		
		// Two ranges to get full color spectrum
		cvInRangeS(imghsv3, cvScalar(0, 100, 100,0), cvScalar(10, 255, 255, 0), hueLower);
		cvInRangeS(imghsv3, cvScalar(160, 100, 100, 0), cvScalar(179, 255, 255, 0), hueUpper);
		cvAddWeighted(hueLower, 1.0, hueUpper, 1.0, 0.0, imgbin3);
		
		cvReleaseImage(hueLower);
		cvReleaseImage(hueUpper);
		cvReleaseImage(imghsv3);

		cvFindContours(imgbin3, storage, contour1, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS,
				cvPoint(0, 0));
	
	
		contour2 = contour1;

		while (contour1 != null && !contour1.isNull()) {
			areaC = cvContourArea(contour1, CV_WHOLE_SEQ, 1);
			if (areaC > areaMax)
				areaMax = areaC;
			contour1 = contour1.h_next();

		}

		while (contour2 != null && !contour2.isNull()) {
			areaC = cvContourArea(contour2, CV_WHOLE_SEQ, 1);
			if (areaC < areaMax) {
				cvDrawContours(imgbin3, contour2, CV_RGB(0, 0, 0), CV_RGB(0, 0, 0), 0, CV_FILLED, 8, cvPoint(0, 0));
			}
			contour2 = contour2.h_next();
		}
		// cvSmooth(imgbin, imgbin, 3, smoother, 0, 0, 0);
		return imgbin3;
	}

	public IplImage findContoursGreen(IplImage img) {
		
		// Green
		CvScalar minc = cvScalar(35, 70, 7, 0), maxc = cvScalar(75, 255, 255, 0);
		CvSeq contour1 = new CvSeq(), contour2;
		CvMemStorage storage = CvMemStorage.create();
		double areaMax = 1000, areaC = 0;

		imghsv4 = cvCreateImage(cvGetSize(img), 8, 3);
		imgbin4 = cvCreateImage(cvGetSize(img), 8, 1);
		
		cvCvtColor(img, imghsv4, CV_BGR2HSV);
		cvInRangeS(imghsv4, minc, maxc, imgbin4);
	
			
			
		cvFindContours(imgbin4, storage, contour1, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS,
				cvPoint(0, 0));

		
		contour2 = contour1;

		while (contour1 != null && !contour1.isNull()) {
			areaC = cvContourArea(contour1, CV_WHOLE_SEQ, 1);
			if (areaC > areaMax)
				areaMax = areaC;
			contour1 = contour1.h_next();

		}

		while (contour2 != null && !contour2.isNull()) {
			areaC = cvContourArea(contour2, CV_WHOLE_SEQ, 1);
			if (areaC < areaMax) {
				cvDrawContours(imgbin4, contour2, CV_RGB(0, 0, 0), CV_RGB(0, 0, 0), 0, CV_FILLED, 8, cvPoint(0, 0));
			}
			contour2 = contour2.h_next();
		}

		return imgbin4;

	}

	public IplImage opticalFlowOnDrones(IplImage imgA, IplImage newFrame) {
		// Load two images and allocate other structures
		CvSize cvSize = cvSize(imgA.width(), imgA.height());

		imgB = cvCreateImage(cvSize, newFrame.depth(), 1);
		cvCvtColor(newFrame, imgB, CV_BGR2GRAY);

		imgC = cvCreateImage(cvSize, newFrame.depth(), 1);
		cvCopy(imgA, imgC);
		
		cvThreshold(imgC, imgC, 100, 255, CV_THRESH_TOZERO);

		CvSize img_sz = cvGetSize(imgA);
		int win_size = 15;

		eig_image = cvCreateImage(img_sz, IPL_DEPTH_32F, 1);
		tmp_image = cvCreateImage(img_sz, IPL_DEPTH_32F, 1);

		IntPointer corner_count = new IntPointer(1).put(MAX_CORNERS);
		CvPoint2D32f cornersA = new CvPoint2D32f(MAX_CORNERS);

		CvArr mask = null;
		cvGoodFeaturesToTrack(imgA, eig_image, tmp_image, cornersA, corner_count, 0.05, 5.0, mask, 3, 0, 0.04);

		cvFindCornerSubPix(imgA, cornersA, corner_count.get(), cvSize(win_size, win_size), cvSize(-1, -1),
				cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03));

		// Call Lucas Kanade algorithm
		BytePointer features_found = new BytePointer(MAX_CORNERS);
		FloatPointer feature_errors = new FloatPointer(MAX_CORNERS);

		CvSize pyr_sz = cvSize(imgA.width() + 8, imgB.height() / 3);

		pyrA = cvCreateImage(pyr_sz, IPL_DEPTH_32F, 1);
		pyrB = cvCreateImage(pyr_sz, IPL_DEPTH_32F, 1);

		CvPoint2D32f cornersB = new CvPoint2D32f(MAX_CORNERS);

		cvCalcOpticalFlowPyrLK(imgA, imgB, pyrA, pyrB, cornersA, cornersB, corner_count.get(),
				cvSize(win_size, win_size), 5, features_found, feature_errors,
				cvTermCriteria(CV_TERMCRIT_NUMBER | CV_TERMCRIT_NUMBER, 20, 0.3), 0);

		// Put lines on the screen along with dots
		for (int i = 0; i < corner_count.get(); i++) {
			if (features_found.get(i) == 0 || feature_errors.get(i) > 550) {
				continue;
			}
			cornersA.position(i);
			cornersB.position(i);
			CvPoint p0 = cvPoint(Math.round(cornersA.x()), Math.round(cornersA.y()));
			CvPoint p1 = cvPoint(Math.round(cornersB.x()), Math.round(cornersB.y()));
			cvLine(imgC, p0, p1, CV_RGB(255, 255, 255), 3, CV_AA, 0);
			
			if (!p0.toString().equals(p1.toString())) {
				Vector v0 = convertToVector(p0.toString());
				Vector v1 = convertToVector(p1.toString());
				Vector newVector = v0.subtract(v1);
				
				if(newVector.y < -10){
					System.out.println("Moving Down");
				}
				
				if(newVector.y > 10){
					System.out.println("Moving Up");
				}
				
				if(newVector.x > 10){
					System.out.println("Moving Left");
				}
				
				if(newVector.x < -10){
					System.out.println("Moving Right");
				}
			}
		}
		return imgC;
	}

	public synchronized IplImage findPolygons(IplImage coloredImage, IplImage filteredImage, int edgeNumber) {
		cvClearMemStorage(storage);
		// coloredImage = balanceWhite(coloredImage);
		CvSeq contour = new CvSeq(null);
		cvFindContours(filteredImage, storage, contour, Loader.sizeof(CvContour.class), CV_RETR_LIST,
				CV_CHAIN_APPROX_SIMPLE);

		// scale of center box
		int factor = 4;

		// find center points
		xleft = (int) coloredImage.width() / factor;
		xright = (int) (coloredImage.width() / factor) * (factor - 1);
		ytop = 0;
		ybot = coloredImage.height();
		// center of centerpoints y
		yCenterBottom = (coloredImage.height() / 3) * 2;
		yCenterTop = (coloredImage.height() / 3);

		// Find red point
		int posX = 0;
		int posY = 0;
		IplImage detectThrs = getThresholdImage(filteredImage);
		CvMoments moments = new CvMoments();
		cvMoments(detectThrs, moments, 1);
		double mom10 = cvGetSpatialMoment(moments, 1, 0);
		double mom01 = cvGetSpatialMoment(moments, 0, 1);
		double area = cvGetCentralMoment(moments, 0, 0);
		posX = (int) (mom10 / area);
		posY = (int) (mom01 / area);

		while (contour != null && !contour.isNull()) {
			if (contour.elem_size() > 0) {
				CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
						cvContourPerimeter(contour) * 0.02, 0);
				if (points.total() == edgeNumber && cvContourArea(points) > 150 && cvContourArea(points) < 10000) {
					// drawLines of Box
					cvDrawContours(coloredImage, points, CvScalar.WHITE, CvScalar.WHITE, -2, 2, CV_AA);
					// Counter for checking points in center box
				}
			}
			contour = contour.h_next();
		}
		return coloredImage;
	}

	public synchronized IplImage findQRFrames(IplImage coloredImage, IplImage filteredImage) {
		float known_distance = 100;
		float known_width = 27;
		float focalLength = (167 * known_distance) / known_width;

		cvClearMemStorage(storage);
		CvSeq contour = new CvSeq(null);
		cvFindContours(filteredImage, storage, contour, Loader.sizeof(CvContour.class), CV_RETR_LIST,
				CV_CHAIN_APPROX_SIMPLE);

		CvBox2D[] markers = new CvBox2D[3];
		markers[0] = new CvBox2D();
		markers[1] = new CvBox2D();
		markers[2] = new CvBox2D();

		int codeIndex = 0;
		while (contour != null && !contour.isNull()) {
			if (contour.elem_size() > 0) {
				CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
						cvContourPerimeter(contour) * 0.02, 0);
				if (points.total() == 4 && cvContourArea(points) > 50) {
					markers[codeIndex] = cvMinAreaRect2(contour, storage);
					IplImage img1 = IplImage.create(coloredImage.width(), coloredImage.height(), coloredImage.depth(),
							1);
					cvCvtColor(coloredImage, img1, CV_RGB2GRAY);
					cvCanny(img1, img1, 100, 200);
					IplImage mask = IplImage.create(coloredImage.width(), coloredImage.height(), IPL_DEPTH_8U,
							coloredImage.nChannels());
					cvDrawContours(mask, contour, CvScalar.WHITE, CV_RGB(248, 18, 18), 1, -1, 8);
					IplImage crop = IplImage.create(coloredImage.width(), coloredImage.height(), IPL_DEPTH_8U,
							coloredImage.nChannels());

					//
					cvCopy(coloredImage, crop, mask);
					return crop;
				}
			}
			contour = contour.h_next();
		}
		return null;
	}

	@SuppressWarnings("resource")
	public synchronized IplImage fRFrames(IplImage image) {
		float known_distance = 100;
		float known_width = 27;
		float focalLength = (167 * known_distance) / known_width;

		cvClearMemStorage(storage);
		// image = balanceWhite(image);
		IplImage grayImage = IplImage.create(image.width(), image.height(), IPL_DEPTH_8U, image.nChannels());
		cvCvtColor(image, grayImage, CV_BGR2GRAY);
		IplImage orgImage = IplImage.create(image.width(), image.height(), IPL_DEPTH_8U, image.nChannels());
		cvCopy(image, orgImage);
		// grayImage = getThresholdBlackImage(grayImage);

		CvSeq contour = new CvSeq(null);
		cvFindContours(grayImage, storage, contour, Loader.sizeof(CvContour.class), CV_RETR_LIST,
				CV_CHAIN_APPROX_SIMPLE);

		// center dots
		int factor = 3;

		// find center points
		xleft = (int) image.width() / factor;
		xright = (int) (image.width() / factor) * (factor - 1);
		ytop = (int) image.height() / factor;
		ybot = (int) (image.height() / factor) * (factor - 1);

		// Make center points
		CvPoint pointTopLeft = cvPoint(xleft, ytop);
		CvPoint pointBottomLeft = cvPoint(xleft, ybot);
		CvPoint pointTopRight = cvPoint(xright, ytop);
		CvPoint pointRightBottom = cvPoint(xright, ybot);

		// Find red point
		int posX = 0;
		int posY = 0;
		IplImage detectThrs = getThresholdImage(grayImage);
		CvMoments moments = new CvMoments();
		cvMoments(detectThrs, moments, 1);
		double mom10 = cvGetSpatialMoment(moments, 1, 0);
		double mom01 = cvGetSpatialMoment(moments, 0, 1);
		double area = cvGetCentralMoment(moments, 0, 0);
		posX = (int) (mom10 / area);
		posY = (int) (mom01 / area);
		CvBox2D[] markers = new CvBox2D[3];
		markers[0] = new CvBox2D();
		markers[1] = new CvBox2D();
		markers[2] = new CvBox2D();
		IplImage crop = IplImage.create(orgImage.width(), orgImage.height(), IPL_DEPTH_8U, orgImage.nChannels());
		cvSetZero(crop);
		int codeIndex = 0;

		while (contour != null && !contour.isNull()) {

			// Draw red point
			cvLine(image, pointTopLeft, pointTopRight, CV_RGB(255, 0, 255), 3, CV_AA, 0);
			cvLine(image, pointTopRight, pointRightBottom, CV_RGB(255, 0, 255), 3, CV_AA, 0);
			cvLine(image, pointRightBottom, pointBottomLeft, CV_RGB(255, 0, 255), 3, CV_AA, 0);
			cvLine(image, pointBottomLeft, pointTopLeft, CV_RGB(255, 0, 255), 3, CV_AA, 0);

			if (contour.elem_size() > 0) {
				CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
						cvContourPerimeter(contour) * 0.02, 0);
				if (cvContourArea(points) > 100) {
					for (int i = 0; i < points.total(); i++) {
						//// cvLine(image, p0, p0, CV_RGB(255, 0, 0), 3, CV_AA,
						//// 0);
						CvPoint v = new CvPoint(cvGetSeqElem(points, i));
						cvDrawContours(image, points, CvScalar.RED, CvScalar.RED, -2, 2, CV_AA);
						CvPoint p0 = cvPoint(posX, posY);
						// Draw red point

						markers[codeIndex] = cvMinAreaRect2(contour, storage);
						IplImage img1 = IplImage.create(orgImage.width(), orgImage.height(), orgImage.depth(), 1);
						cvCvtColor(orgImage, img1, CV_RGB2GRAY);
						cvCanny(img1, img1, 100, 200);

						IplImage mask = IplImage.create(orgImage.width(), orgImage.height(), IPL_DEPTH_8U,
								orgImage.nChannels());
						cvDrawContours(mask, contour, CvScalar.WHITE, CV_RGB(248, 18, 18), 1, -1, 8);

						cvCopy(orgImage, crop, mask);
						return mask;
					}
				}
			}

			contour = contour.h_next();
		}
		return image;
	}

	private int checkPositionInCenter(int posx, int posy) {
		boolean bottomCenterCondition = posy > yCenterBottom;
		boolean upperCenterCondition = posy < yCenterTop;
		if (upperCenterCondition) {
			return 1;
		}

		if (!bottomCenterCondition && !upperCenterCondition) {
			return 2;
		}

		if (bottomCenterCondition) {
			return 3;
		}

		return 0;

	}

	private boolean checkBoxForCenter(int posx, int posy) {

		boolean verticalCondition = posy > ytop && posy < ybot;
		boolean horizontalCondition = posx > xleft && posx < xright;
		if (horizontalCondition && verticalCondition) {
			return true;
		} else {
			// System.out.println("not centered");
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
			// System.out.println("not centered");
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

	private IplImage getThresholdImage(IplImage orgImg) {
		IplImage imgThreshold = cvCreateImage(cvGetSize(orgImg), 8, 1);
		rgba_min = cvScalar(minRed, minGreen, minBlue, 0);
		rgba_max = cvScalar(maxRed, maxGreen, maxBlue, 0);

		//// System.out.println("RGBMIN R " + rgba_min.red() + "G " +
		//// rgba_min.green() + " B " + rgba_min.blue()
		// + "Smoothing: " + smoother);
		// System.out.println("RGBMAX R " + rgba_max.red() + "G " +
		//// rgba_max.green() + " B " + rgba_max.blue()
		// + "Smoothing: " + smoother);
		cvInRangeS(orgImg, rgba_min, rgba_max, imgThreshold);// red
		cvSmooth(imgThreshold, imgThreshold, 2, smoother, 0, 0, 0);
		// cvSaveImage(++ii + "dsmthreshold.jpg", imgThreshold);
		return imgThreshold;
	}

	private IplImage getThresholdWhiteImage(IplImage orgImg) {
		IplImage imgThreshold = cvCreateImage(cvGetSize(orgImg), 8, 1);
		rgba_min = CvScalar.GRAY;
		rgba_max = CvScalar.WHITE;

		//// System.out.println("RGBMIN R " + rgba_min.red() + "G " +
		//// rgba_min.green() + " B " + rgba_min.blue()
		// + "Smoothing: " + smoother);
		// System.out.println("RGBMAX R " + rgba_max.red() + "G " +
		//// rgba_max.green() + " B " + rgba_max.blue()
		// + "Smoothing: " + smoother);
		cvInRangeS(orgImg, rgba_min, rgba_max, imgThreshold);// red
		cvSmooth(imgThreshold, imgThreshold, 2, smoother, 0, 0, 0);
		// cvSaveImage(++ii + "dsmthreshold.jpg", imgThreshold);
		return imgThreshold;
	}

	private IplImage balanceWhite(IplImage cvtImg) {
		// IplImage cvtImg = IplImage.create(newImg.width(), newImg.height(),
		// newImg.depth(), newImg.nChannels());
		// cvCvtColor(newImg, cvtImg,CV_BGR2HSV);

		IplImage channel1 = IplImage.create(cvtImg.width(), cvtImg.height(), cvtImg.depth(), 1);
		IplImage channel2 = IplImage.create(cvtImg.width(), cvtImg.height(), cvtImg.depth(), 1);
		IplImage channel3 = IplImage.create(cvtImg.width(), cvtImg.height(), cvtImg.depth(), 1);
		cvSplit(cvtImg, channel1, channel2, channel3, null);
		cvEqualizeHist(channel1, channel1);
		cvEqualizeHist(channel2, channel2);
		cvEqualizeHist(channel3, channel3);
		cvMerge(channel1, channel2, channel3, null, cvtImg);

		// cvCvtColor(cvtImg, newImg, CV_HSV2BGR);

		return cvtImg;
	}
	
	public IplImage erodeAndDilate(IplImage thresh)
	{
		// Removes static
		  Mat matImg = cvarrToMat(thresh);
		  Mat eroded = new Mat(MORPH_RECT,3,3);				  
		  erode(matImg, eroded, eroded);
		  
		  // Dilate image, by default 3x3 element is used
		  Mat dilated = new Mat(MORPH_RECT,8,8);
		  dilate(eroded, eroded, dilated);
		  
		  thresh = new IplImage(eroded);
		
		return thresh;
		
	}
}