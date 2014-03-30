//
// Cleversafe open-source code header - Version 1.3 - January 2, 2009
//
// Cleversafe Dispersed Storage(TM) is software for secure, private and
// reliable storage of the world's data using information dispersal.
//
// Copyright (C) 2005-2009 Cleversafe, Inc. All rights reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
// USA.
//
// Contact Information: Cleversafe, 224 North Desplaines Street, Suite 500
// Chicago, IL 60661, USA.
// email licensing@cleversafe.org
//
// END-OF-HEADER
//
// -----------------------
// @author: ilya
//
// Date: Jun 13, 2010
// ---------------------

package com.cleversafe.oom.cli;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.codec.binary.Hex;

public class OOMReader
{
   private static int ID_LENGTH = 18;

   /**
    * @param args
    * @throws IOException
    */
   public static void main(final String[] args) throws IOException
   {
      if (args.length < 1)
      {
         System.out.println("Usage: file1 ... [fileN]");
         System.exit(1);
      }
      for (int i = 0; i < args.length; i++)
      {
         OOMReader.readIdFile(args[i]);
      }
   }

   public static void readIdFile(final String filename) throws IOException
   {
      FileInputStream in = null;
      try
      {
         in = new FileInputStream(filename);
         final byte[] objectID = new byte[ID_LENGTH];
         int numRecords = 0;
         while (in.read(objectID, 0, ID_LENGTH) == ID_LENGTH)
         {
            numRecords++;
            final String id = new String(Hex.encodeHex(objectID));
            System.out.println(id);
         }
         System.out.println(numRecords + " records");
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }
   }
}
