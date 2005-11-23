/*
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * All rights reserved.
 * 
 * Contact: howl@objectweb.org
 * 
 * This software is licensed under the BSD license.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *     
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * ------------------------------------------------------------------------------
 * $Id: Version.java,v 1.5 2005-11-23 17:56:52 girouxm Exp $
 * ------------------------------------------------------------------------------
 */
package org.objectweb.howl.log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version {
  private static final String cvsId = "$Id: Version.java,v 1.5 2005-11-23 17:56:52 girouxm Exp $";
  
  void run()
  {
    boolean verbose = Boolean.getBoolean("verbose");
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("cvs/status.txt");
    if (is == null) 
    {
      System.err.println("cvs/status.txt not found; version information not available.\n" + cvsId);
      return;
    }
    
    byte[] data = new byte[100];
    String residue = "";
    Pattern p = Pattern.compile("^.*Repository revision:[ \\t]+(.+)[ \\t]+.*src/java/((.+/)*)(.+\\.java),v");
    String currentPackage = "";
    try {
      while (is.available() > 0) {
        int bytesread = is.read(data);
        if (verbose) {
          System.out.print(new String(data, 0, bytesread));
        }
        else {
          String s = residue.concat(new String(data, 0, bytesread));
          int fromIndex = 0;
          for (int i = s.indexOf('\n', fromIndex); i >= 0; i = s.indexOf('\n', fromIndex)) {
            String line = s.substring(fromIndex, i);
            Matcher m = p.matcher(line);
            if (m.matches())
            {
              String revision = m.group(1);
              String pkg = m.group(2).replace('/','.');
              if (! pkg.equals(currentPackage))
              {
                currentPackage = pkg;
                int len = pkg.length() - 1;
                System.out.println(pkg.substring(0,len));
              }
              String module = m.group(4);
              int length = 40 - module.length();
              if (length < 0) length = 0; 
              char[] fill = new char[length];
              Arrays.fill(fill, ' ');
              System.out.println("    " + module + new String(fill) + revision);
            }
            fromIndex = i + 1;
          }
          residue = s.substring(fromIndex);
        }
      }
    } catch (IOException e) {
      System.err.println(e);
    }
  }

  public static void main(String[] args) {
    new Version().run();
  }

}
