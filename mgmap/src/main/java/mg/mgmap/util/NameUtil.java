/*
 * Copyright 2017 - 2020 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.util;

import android.util.Log;

import mg.mgmap.MGMapApplication;

/**
 * Utility to determine the class name dynamically.
 * This is usually used fog logging purposes.
 */
public class NameUtil {
	 
	public static String getCurrentClassName(){
		return new Throwable().getStackTrace()[1].getClassName();
	}
	
	public static String getCurrentMethodName(){
		return new Throwable().getStackTrace()[1].getMethodName();
	}

	public static String getCurrentStackName(){
		StackTraceElement ste = new Throwable().getStackTrace()[1];
		return ste.getClassName()+"."+ste.getMethodName()+"("+ste.getFileName()+":"+ste.getLineNumber()+")";
	}

	public static String context(){
		StackTraceElement ste = new Throwable().getStackTrace()[1];
		return ste.getClassName()+"."+ste.getMethodName()+"("+ste.getFileName()+":"+ste.getLineNumber()+") ";
	}

	public static String[] context(int num){
		String[] steArray = new String[num];
		for (int i=0; i<num; i++){
			StackTraceElement ste = new Throwable().getStackTrace()[1+i];
			steArray[i] = ste.getClassName()+"."+ste.getMethodName()+"("+ste.getFileName()+":"+ste.getLineNumber()+") ";
		}
		return steArray;
	}
	public static void logContext(int num){
		for (int i=0; i<num; i++){
			StackTraceElement ste = new Throwable().getStackTrace()[1+i];
			Log.d(MGMapApplication.LABEL, "     Context: "+ste.getClassName()+"."+ste.getMethodName()+"("+ste.getFileName()+":"+ste.getLineNumber()+") ");
		}
	}

	public static String getTag(){
		return "MGMapViewer "+new Throwable().getStackTrace()[1].getClassName();
	}





}
