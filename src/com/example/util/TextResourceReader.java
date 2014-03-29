package com.example.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

import android.content.Context;
import android.content.res.Resources;

public class TextResourceReader {

	public static String readTextFileFromResource(Context context, int resourceId) {
		StringBuilder body = new StringBuilder();
		
		try {
			InputStream inputStream = context.getResources().openRawResource(resourceId);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);			
			
			String nextLine;
			while ((nextLine = bufferedReader.readLine()) != null) {
				if (!nextLine.matches("\\s*//.*")) {
					body.append(nextLine);
					body.append('\n');
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Could not open resource no." + resourceId, e);
		}
		catch (Resources.NotFoundException nfe) {
			throw new RuntimeException("Resource not found: " + resourceId, nfe);
		}
		
		return body.toString();
		
	}

	public static int readNextInt(Scanner scan) {
		while (!scan.hasNextInt()) {
			scan.next();
		}
		return scan.nextInt();
	}
	
	public static float readNextFloat(Scanner scan) {
		while (!scan.hasNextFloat()) {
			scan.next();
		}
		return scan.nextFloat();
	}
}