package io.mosip.registration.processor.stages.ocr_document_classifier;


import io.mosip.registration.processor.packet.storage.dto.Document;
import net.sourceforge.tess4j.Tesseract;
import org.apache.hadoop.util.hash.Hash;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TesseractOCR {

    private static Tesseract getTessaract()
    {
        Tesseract instance = new Tesseract();
        instance.setDatapath("D:");
        /*
            public void setDatapath(java.lang.String datapath)
        --> Sets path to tessdata.
            Parameters: datapath - the tessdata path to set

         */

        instance.setLanguage("eng");
        /*
            public void setLanguage(java.lang.String language)
        --> Sets language for OCR.
            Parameters: language - the language code, which follows ISO 639-3 standard.

         */
        return instance;
    }

    // manually choosing substrings for each field. may not work when tesseract doesnt work properly and adds new characters
    // or misses some characters.
    /*
           ways tried to improve tesseract accuracy:
           1. DPI
           2. changing text size

           other ways:
           1. using other image pre-processing tools like - leptonica, ImageMagick, OpenCV, Unpaper.
           2. or using Java open CV libraries to pre-process image.



           to ways to get better results:
           1. improve tesseract accuracy
           2. take the field and do pattern matching in the obtained tesseract output and
           put a threshhold and if above threshhold accept it else reject.
     */
    // other way to do is, take in the name provided by
    public static Map<String, String> parsePassportTD3(String line1, String line2, String...line3)
    {

//        System.out.println(line1 + "\n\n length of line1 = " + line1.length() + "\n\n");
//        System.out.println(line2 + "\n\n length of line2 = " + line1.length() + "\n\n");
//        System.out.println("\n\n\n\n");
//        System.out.println(line2 + " length = " + line2.length());
//        System.out.println(line3[0] + " length = " + line3[0].length());

//        System.out.println("The MRZ portion extracted from the document is:\n");
//        System.out.println(line1);
//        System.out.println(line2);
        //System.out.println(line3[0] + "\t length = " + line3[0].length());

        Map<String, String> details = new HashMap<String, String>();
        // document code
        String[] documentCode = line1.substring(0, 2).split("<");
        details.put("DocumentCode", documentCode[0]);

        String[] countryCode = line1.substring(2, 5).split("<");
        details.put("CountryCode",countryCode[0]);

        String[] fullName = line1.substring(5).split("<<");
        String name="";
        for(String s:fullName)
        {
            //System.out.println(s);
            name += s.replace('<',' ')+" "; // fullName[0] -> surname, fullName[1] -> firstName first half
        } // extra space at end.

        name=name.trim();
        details.put("Name",name);

        //System.out.println("\n\n\n");


        String[] documentNumber = line2.substring(0, 9).split("<");
        details.put("DocumentNumber", documentNumber[0]);

        String[] dateOfBirth = line2.substring(14, 20).split("<");
        details.put("DOB", dateOfBirth[0]);

        String[] gender = line2.substring(21, 22).split("<");
        details.put("Gender", gender[0]);

        String[] dateOfExpiry = line2.substring(22).split("<");
        //System.out.println(dateOfExpiry);
        details.put("dateOfExpiry", dateOfExpiry[0]);

        //for(String s: result)
        //    System.out.println(s);    // documentCode, countryCode, fullName, documentNumber, dateOfBirth, gender, dateOfExpiry

        return details;
    }

    // getdetails() --> map<> : name -> value, age -> value
    public Map<String, String> getDetails(Document doc) throws Exception
    {

        byte [] data = doc.getDocument();
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        BufferedImage bImage2 = ImageIO.read(bis);

        Tesseract tesseract = getTessaract();
        //File file=new File("C:\\Users\\HP\\IdeaProjects\\test\\Indonesian_passport_data_page.jpg");

        String result= tesseract.doOCR(bImage2);
        /*
            http://tess4j.sourceforge.net/docs/docs-1.5/net/sourceforge/tess4j/Tesseract.html#doOCR-java.awt.image.BufferedImage-

            public java.lang.String doOCR(java.awt.image.BufferedImage bi) throws TesseractException
        --> Performs OCR operation. <--
            Parameters: bi - a buffered image
            Returns: the recognized text
            Throws: TesseractException

         */

        //System.out.println(result + "\n\n\n");

        String[] splitResult = result.split("\n");
        int lengthOfResult = splitResult.length;

        //System.out.println(splitResult[lengthOfResult-2] + "   length = " + splitResult[lengthOfResult-2].length() + "\n"+ splitResult[lengthOfResult-1] + "   length = " + splitResult[lengthOfResult-1].length());

        int i;
        for(i=lengthOfResult-1;i>=0;i--)
        {
            if(splitResult[i].charAt(0) == 'P' || splitResult[i].charAt(0) == 'p')
            {
                break;
            }
        }
        Map<String, String> fullData = parsePassportTD3(splitResult[i], splitResult[i+1]);
        // documentCode, countryCode, fullName, documentNumber, dateOfBirth, gender, dateOfExpiry

        //for(Map.Entry<String, String> entry: fullData.entrySet())
        //    System.out.println(entry.getKey() + " --> " +entry.getValue());

        //ImageIO.write(bImage2, "jpg", new File("output.jpg") );
        //System.out.println("image created");

        return fullData;
    }
}


