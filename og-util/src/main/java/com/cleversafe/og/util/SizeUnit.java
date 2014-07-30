//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Apr 1, 2014
// ---------------------

package com.cleversafe.og.util;

/**
 * A SizeUnit represents sizes at a given unit of granularity and provides a utility method to
 * convert to bytes.
 * 
 * @since 1.0
 */
public enum SizeUnit
{
   BYTES {
      @Override
      public long toBytes(final long s)
      {
         return s;
      }
   },
   KILOBYTES {
      @Override
      public long toBytes(final long s)
      {
         return x(s, SI1 / SI0, MAX / (SI1 / SI0));
      }
   },
   KIBIBYTES {
      @Override
      public long toBytes(final long s)
      {
         return x(s, IEC1 / IEC0, MAX / (IEC1 / IEC0));
      }
   },
   MEGABYTES {
      @Override
      public long toBytes(final long s)
      {
         return x(s, SI2 / SI0, MAX / (SI2 / SI0));
      }
   },
   MEBIBYTES {
      @Override
      public long toBytes(final long s)
      {
         return x(s, IEC2 / IEC0, MAX / (IEC2 / IEC0));
      }
   },
   GIGABYTES {
      @Override
      public long toBytes(final long s)
      {
         return x(s, SI3 / SI0, MAX / (SI3 / SI0));
      }
   },
   GIBIBYTES {
      @Override
      public long toBytes(final long s)
      {
         return x(s, IEC3 / IEC0, MAX / (IEC3 / IEC0));
      }
   },
   TERABYTES {
      @Override
      public long toBytes(final long s)
      {
         return x(s, SI4 / SI0, MAX / (SI4 / SI0));
      }
   },
   TEBIBYTES {
      @Override
      public long toBytes(final long s)
      {
         return x(s, IEC4 / IEC0, MAX / (IEC4 / IEC0));
      }
   },
   PETABYTES {
      @Override
      public long toBytes(final long s)
      {
         return x(s, SI5 / SI0, MAX / (SI5 / SI0));
      }
   },
   PEBIBYTES {
      @Override
      public long toBytes(final long s)
      {
         return x(s, IEC5 / IEC0, MAX / (IEC5 / IEC0));
      }
   };

   // IEC Units
   private static final long IEC0 = 1L;
   private static final long IEC1 = IEC0 * 1024L;
   private static final long IEC2 = IEC1 * 1024L;
   private static final long IEC3 = IEC2 * 1024L;
   private static final long IEC4 = IEC3 * 1024L;
   private static final long IEC5 = IEC4 * 1024L;

   // SI Units
   private static final long SI0 = 1L;
   private static final long SI1 = SI0 * 1000L;
   private static final long SI2 = SI1 * 1000L;
   private static final long SI3 = SI2 * 1000L;
   private static final long SI4 = SI3 * 1000L;
   private static final long SI5 = SI4 * 1000L;

   private static final long MAX = Long.MAX_VALUE;

   /**
    * Scale s by m, checking for overflow. This has a short name to make above code more readable.
    */
   private static long x(final long s, final long m, final long over)
   {
      if (s > over)
         return Long.MAX_VALUE;
      if (s < -over)
         return Long.MIN_VALUE;
      return s * m;
   }

   /**
    * Converts this unit to bytes
    * 
    * @param size
    *           the size with this unit
    * @return size, in bytes
    */
   public long toBytes(final long size)
   {
      throw new AbstractMethodError();
   }
}
