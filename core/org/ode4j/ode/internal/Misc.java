/*************************************************************************
 *                                                                       *
 * Open Dynamics Engine, Copyright (C) 2001,2002 Russell L. Smith.       *
 * All rights reserved.  Email: russ@q12.org   Web: www.q12.org          *
 *                                                                       *
 * This library is free software; you can redistribute it and/or         *
 * modify it under the terms of EITHER:                                  *
 *   (1) The GNU Lesser General Public License as published by the Free  *
 *       Software Foundation; either version 2.1 of the License, or (at  *
 *       your option) any later version. The text of the GNU Lesser      *
 *       General Public License is included with this library in the     *
 *       file LICENSE.TXT.                                               *
 *   (2) The BSD-style license that is included with this library in     *
 *       the file LICENSE-BSD.TXT.                                       *
 *                                                                       *
 * This library is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files    *
 * LICENSE.TXT and LICENSE-BSD.TXT for more details.                     *
 *                                                                       *
 *************************************************************************/
package org.ode4j.ode.internal;

import org.cpp4j.FILE;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DQuaternionC;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;

import static org.cpp4j.Cstdio.*;



/** ****************************************************************************
 * random numbers
 */
public class Misc extends Common {

	//	static unsigned long seed = 0;  //32bit unsigned
	private static long seed = 0;

	protected Misc() {
		//private
	}
	
	
	//unsigned long dRand()
	public static long
	dRand()
	{
		seed = (1664525L*seed + 1013904223L) & 0xffffffffL;
//		seed *= 1664525L;
//		//if (seed > 0xffffffff) seed -= 0xffffffff+1;
////		seed &= 0xffffffff;  //Could be skipped
//		seed += 1013904223L;
//		if (seed > 0xffffffff) seed -= 0xffffffff+1;
//		seed = seed & 0x000000ffffffffL;  //Could be skipped
		return seed;
	}


	//unsigned long  dRandGetSeed()
	public static long  dRandGetSeed()
	{
		return Math.abs(seed);
	}


	//void dRandSetSeed (unsigned long s)
	public static void dRandSetSeed (long s)
	{
		seed = s;
	}


	public static boolean dTestRand()
	{
		//	  unsigned long oldseed = seed;
		long oldseed = seed;
		boolean ret = true;
		seed = 0;
		if (dRand() != 0x3c6ef35f || dRand() != 0x47502932 ||
				dRand() != 0xd1ccf6e9L || dRand() != 0xaaf95334L ||
				dRand() != 0x6252e503L ||
				dRand() != 0x9f2ec686L ||//) ret = false;
				dRand() != 0x57fe6c2dL ||
				dRand() != 0xa3d95fa8L ||
				dRand() != 0x81fdbee7L ||
				dRand() != 0x94f0af1aL ||
				dRand() != 0xcbf633b1L) ret = false;
		seed = oldseed;
		return ret;
	}


	/**
	 *  adam's all-int straightforward(?) dRandInt (0..n-1)
	 * TODO TZ Check whether this is ported correctly from unsigned long.
	 */
	public static int dRandInt (long n)
	{
		// seems good; xor-fold and modulus
		//  final unsigned long un = n;
		//  unsigned long r = dRand();
		final long un = n;
		long r = dRand();

		// note: probably more aggressive than it needs to be -- might be
		//       able to get away without one or two of the innermost branches.
//		if (un <= 0x00010000UL) {
//			r ^= (r >> 16);
//			if (un <= 0x00000100UL) {
//				r ^= (r >> 8);
//				if (un <= 0x00000010UL) {
//					r ^= (r >> 4);
//					if (un <= 0x00000004UL) {
//						r ^= (r >> 2);
//						if (un <= 0x00000002UL) {
//							r ^= (r >> 1);
//						}
//					}
//				}
//			}
//		}
		if (un <= 0x00010000L) {
			r ^= (r >> 16);
			if (un <= 0x00000100L) {
				r ^= (r >> 8);
				if (un <= 0x00000010L) {
					r ^= (r >> 4);
					if (un <= 0x00000004L) {
						r ^= (r >> 2);
						if (un <= 0x00000002L) {
							r ^= (r >> 1);
						}
					}
				}
			}
		}

		return (int) (r % un);    
	}


	public static double dRandReal()
	{
		return ((double) dRand()) / ((double) 0xffffffffL);
	}

	//****************************************************************************
	// matrix utility stuff

//	void dPrintMatrix (final double []A, int n, int m, char []fmt, FILE f)
	void dPrintMatrix (final double []A, int n, int m, String fmt, FILE f)
	{
		int skip = dPAD(m);
		for (int i=0; i<n; i++) {
			for (int j=0; j<m; j++) fprintf (f,fmt,A[i*skip+j]);
			fprintf (f,"\n");
		}
	}


	public static void dMakeRandomVector (DVector3 A, double range) {
		dMakeRandomVector(A.v, 3, range);
	}
	public static void dMakeRandomVector (double[]A, int n, double range)
	{
		for (int i=0; i<n; i++) A[i] = (dRandReal()*2.0-1.0)*range;
	}


	public static void dMakeRandomMatrix (double[]A, int n, int m, double range)
	{
		int skip = dPAD(m);
		Matrix.dSetZero (A,n*skip);
		for (int i=0; i<n; i++) {
			for (int j=0; j<m; j++) A[i*skip+j] = (dRandReal()*2.0-1.0)*range;
		}
	}


	public static void dClearUpperTriangle (double[]A, int n)
	{
		int skip = dPAD(n);
		for (int i=0; i<n; i++) {
			for (int j=i+1; j<n; j++) A[i*skip+j] = 0;
		}
	}


	public static double dMaxDifference (final double[]A, final double[]B, int n, int m)
	{
		int skip = dPAD(m);
		double diff,max;
		max = 0;
		for (int i=0; i<n; i++) {
			for (int j=0; j<m; j++) {
				diff = dFabs(A[i*skip+j] - B[i*skip+j]);
				if (diff > max) {
					max = diff;
				}
			}
		}
		return max;
	}
	public static double dMaxDifference (final DMatrix3C A, final DMatrix3C B, int n, int m)
	{
		return dMaxDifference(((DMatrix3)A).v, ((DMatrix3)B).v, n, m);
	}
	public static double dMaxDifference (final DVector3C A, final DVector3C B, int n, int m) {
		return dMaxDifference(((DVector3)A).v, ((DVector3)B).v, n, m);
	}
	public static double dMaxDifference (final DQuaternionC A, final DQuaternionC B, int n, int m) {
		return dMaxDifference(((DQuaternion)A).v, ((DQuaternion)B).v, n, m);
	}


	//double dMaxDifferenceLowerTriangle (final double *A, final double *B, int n)
	public static double dMaxDifferenceLowerTriangle (final double[]A, final double[]B, int n)
	{
		int skip = dPAD(n);
		double diff,max;
		max = 0;
		for (int i=0; i<n; i++) {
			for (int j=0; j<=i; j++) {
				diff = dFabs(A[i*skip+j] - B[i*skip+j]);
				if (diff > max) max = diff;
			}
		}
		return max;
	}
}