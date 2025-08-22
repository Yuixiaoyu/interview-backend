package com.xiaoyu.interview.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfUtils {

    public static List<String> extractParagraphs(InputStream pdfStream) throws IOException {
        List<String> paragraphs = new ArrayList<>();
        PDDocument document = PDDocument.load(pdfStream);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();

        // 按空行拆分段落
        String[] paraArray = text.split("\\r?\\n\\r?\\n");
        for(String p : paraArray){
            p = p.trim();
            if(!p.isEmpty()) paragraphs.add(p);
        }
        return paragraphs;
    }
}
