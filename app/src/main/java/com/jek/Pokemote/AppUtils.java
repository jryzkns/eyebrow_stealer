package com.jek.Pokemote;

import android.content.Context;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AppUtils {

    // CONSTANTS
    private static final int bufferSize = 4096;

    public static final int frameW = 640 - 1;
    public static final int frameH = 360 - 1;


    public static final Scalar WHITE  = new Scalar(255,  255,    255,    100);

    private static Mat blank = new Mat( AppUtils.frameH+1, AppUtils.frameW+1,
                                        CvType.CV_8UC4, Scalar.all(0));

    public static Mat getBlankFrame(){ return blank.clone(); }

    public static void loadFileContext(Context current, File fp, int resId){
        InputStream is = current.getResources().openRawResource(resId);
        try {
            FileOutputStream os = new FileOutputStream(fp);
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
        }catch (Exception e){
            // error
        } finally {
            try{
                is.close();
            } catch (Exception e){
                // error
            }
        }
    }

    public static void putImg(Mat frame, Mat img, Point offset){
        int adjustedX = (int)((offset.x < 0) ? 0 :
                            (offset.x + img.width() >= frameW) ?
                                    frameW - img.width(): offset.x);
        int adjustedY = (int)((offset.y < 0) ? 0 :
                            (offset.y + img.height() >= frameH) ?
                                    frameH - img.height(): offset.y);
        Mat frame_mask = frame.colRange(adjustedX, adjustedX + img.width())
                              .rowRange(adjustedY, adjustedY + img.height());
        Core.add(frame_mask,img,frame_mask);
    }

    public static double clamp (double in, double min, double max){
        return (in < min) ? min : (in > max) ? max : in;
    }

    public static Rect rectExpand(Rect in, double factor){

        int X = (int)clamp((in.tl().x + in.br().x - factor*in.width)/2.,0.,frameW);
        int Y = (int)clamp((in.tl().y + in.br().y - factor*in.height)/2.,0.,frameH);
        int X2 = (int)clamp(X + in.width*factor,0,frameW);
        int Y2 = (int)clamp(Y + in.height*factor,0,frameH);

        return new Rect(X,Y,X2-X,Y2-Y);
    }

}
