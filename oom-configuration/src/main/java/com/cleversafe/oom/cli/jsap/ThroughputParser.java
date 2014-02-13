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
// @author: conor
//
// Date: Feb 18, 2011
// ---------------------

package com.cleversafe.oom.cli.jsap;

import org.cleversafe.jsap.UnitStringParser;

import com.martiansoftware.jsap.ParseException;
import com.martiansoftware.jsap.PropertyStringParser;

public class ThroughputParser extends PropertyStringParser
{

   @Override
   public Object parse(final String arg) throws ParseException
   {
      final String[] pair = arg.split("/");
      final Long numerator = (Long) new UnitStringParser().parse(pair[0]);
      if (pair.length > 1)
      {
         String time = pair[1];
         try
         {
            Float.parseFloat(time.substring(0, 1));
         }
         catch (final NumberFormatException e)
         {
            time = "1" + time;
         }
         final Long denominator = (Long) new UnitStringParser().parse(time);
         return new Long(numerator.longValue() / denominator.longValue());
      }
      else
      {
         throw new ParseException(arg + " contains no denominator, it cannot be a throughput");
      }
   }

}
