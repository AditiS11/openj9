/*
 * Copyright IBM Corp. and others 2022
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] https://openjdk.org/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0 OR GPL-2.0-only WITH OpenJDK-assembly-exception-1.0
 */
package org.openj9.test.jep389.valist;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.AssertJUnit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.CLinker;
import static jdk.incubator.foreign.CLinker.*;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.incubator.foreign.SymbolLookup;
import static jdk.incubator.foreign.CLinker.*;
import jdk.incubator.foreign.ValueLayout;

/**
 * Test cases for JEP 389: Foreign Linker API (Incubator) for the vararg list in upcall.
 */
@Test(groups = { "level.sanity" })
public class UpcallTests {
	private static String osName = System.getProperty("os.name").toLowerCase();
	private static String arch = System.getProperty("os.arch").toLowerCase();
	static boolean isX64 = arch.equals("amd64") || arch.equals("x86_64");
	private static boolean isWinX64 = osName.contains("win") && (arch.equals("amd64") || arch.equals("x86_64"));
	private static boolean isLinuxX64 = osName.contains("linux") && (arch.equals("amd64") || arch.equals("x86_64"));
	private static boolean isLinuxAarch64 = osName.contains("linux") && arch.equals("aarch64");
	/* The padding of struct is not required on Power in terms of VaList */
	private static boolean isStructPaddingNotRequired = arch.startsWith("ppc64");
	private static boolean isAixOS = osName.contains("aix");
	/* long long is 64 bits on AIX/ppc64, which is the same as Windows */
	private static ValueLayout longLayout = (isWinX64 || isAixOS) ? C_LONG_LONG : C_LONG;
	private static CLinker clinker = CLinker.getInstance();

	static {
		System.loadLibrary("clinkerffitests");
	}
	private static final SymbolLookup nativeLibLookup = SymbolLookup.loaderLookup();

	@Test
	public void test_addIntsWithVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addIntsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(int.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromInt(C_INT, 700)
					.vargFromInt(C_INT, 800)
					.vargFromInt(C_INT, 900)
					.vargFromInt(C_INT, 1000), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addIntsFromVaList,
					FunctionDescriptor.of(C_INT, C_INT, C_POINTER), scope);

			int result = (int)mh.invoke(4, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 3400);
		}
	}

	@Test
	public void test_addLongsFromVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addLongsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(long.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromLong(longLayout, 700000L)
					.vargFromLong(longLayout, 800000L)
					.vargFromLong(longLayout, 900000L)
					.vargFromLong(longLayout, 1000000L), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addLongsFromVaList,
					FunctionDescriptor.of(longLayout, C_INT, C_POINTER), scope);

			long result = (long)mh.invoke(4, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 3400000L);
		}
	}

	@Test
	public void test_addDoublesFromVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addDoublesFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(double.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromDouble(C_DOUBLE, 111150.1001D)
					.vargFromDouble(C_DOUBLE, 111160.2002D)
					.vargFromDouble(C_DOUBLE, 111170.1001D)
					.vargFromDouble(C_DOUBLE, 111180.2002D), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addDoublesFromVaList,
					FunctionDescriptor.of(C_DOUBLE, C_INT, C_POINTER), scope);

			double result = (double)mh.invoke(4, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 444660.6006D, 0.0001D);
		}
	}

	@Test
	public void test_addMixedArgsFromVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addMixedArgsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(double.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromInt(C_INT, 700)
					.vargFromLong(longLayout, 800000L)
					.vargFromDouble(C_DOUBLE, 160.2002D), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addMixedArgsFromVaList,
					FunctionDescriptor.of(C_DOUBLE, C_POINTER), scope);

			double result = (double)mh.invoke(vaList, upcallFuncAddr);
			Assert.assertEquals(result, 800860.2002D, 0.0001D);
		}
	}

	@Test
	public void test_addMoreMixedArgsFromVaListByUpcallMH() throws Throwable {
		/* VaList on x86_64(Linux, macOS and Windows) in OpenJDK is unable to handle
		 * the va_list with over 8 arguments (confirmed by OpenJDK/Hotspot). So the
		 * test is disabled for now till the issue is fixed by OpenJDK.
		 */
		if (!isX64) {
			Addressable functionSymbol = nativeLibLookup.lookup("addMoreMixedArgsFromVaListByUpcallMH").get();
			MethodType mt = MethodType.methodType(double.class, VaList.class, MemoryAddress.class);
			FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_POINTER, C_POINTER);
			MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromInt(C_INT, 100)
						.vargFromLong(longLayout, 200000L)
						.vargFromInt(C_INT, 300)
						.vargFromLong(longLayout, 400000L)
						.vargFromInt(C_INT, 500)
						.vargFromLong(longLayout, 600000L)
						.vargFromInt(C_INT, 700)
						.vargFromDouble(C_DOUBLE, 161.2001D)
						.vargFromInt(C_INT, 800)
						.vargFromDouble(C_DOUBLE, 162.2002D)
						.vargFromInt(C_INT, 900)
						.vargFromDouble(C_DOUBLE, 163.2003D)
						.vargFromInt(C_INT, 1000)
						.vargFromDouble(C_DOUBLE, 164.2004D)
						.vargFromInt(C_INT, 1100)
						.vargFromDouble(C_DOUBLE, 165.2005D), scope);
				MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addMoreMixedArgsFromVaList,
						FunctionDescriptor.of(C_DOUBLE, C_POINTER), scope);

				double result = (double)mh.invoke(vaList, upcallFuncAddr);
				Assert.assertEquals(result, 1206216.0015D, 0.0001D);
			}
		}
	}

	@Test
	public void test_addIntsByPtrFromVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addIntsByPtrFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(int.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment intSegmt1 = allocator.allocate(C_INT, 700);
			MemorySegment intSegmt2 = allocator.allocate(C_INT, 800);
			MemorySegment intSegmt3 = allocator.allocate(C_INT, 900);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromAddress(C_POINTER, intSegmt1.address())
					.vargFromAddress(C_POINTER, intSegmt2.address())
					.vargFromAddress(C_POINTER, intSegmt3.address()), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addIntsByPtrFromVaList,
					FunctionDescriptor.of(C_INT, C_INT, C_POINTER), scope);

			int result = (int)mh.invoke(3, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 2400);
		}
	}

	@Test
	public void test_addLongsByPtrFromVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addLongsByPtrFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(long.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment longSegmt1 = allocator.allocate(longLayout, 700000L);
			MemorySegment longSegmt2 = allocator.allocate(longLayout, 800000L);
			MemorySegment longSegmt3 = allocator.allocate(longLayout, 900000L);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromAddress(C_POINTER, longSegmt1.address())
					.vargFromAddress(C_POINTER, longSegmt2.address())
					.vargFromAddress(C_POINTER, longSegmt3.address()), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addLongsByPtrFromVaList,
					FunctionDescriptor.of(longLayout, C_INT, C_POINTER), scope);

			long result = (long)mh.invoke(3, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 2400000L);
		}
	}

	@Test
	public void test_addDoublesByPtrFromVaListByUpcallMH() throws Throwable {
		Addressable functionSymbol = nativeLibLookup.lookup("addDoublesByPtrFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(double.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment doubleSegmt1 = allocator.allocate(C_DOUBLE, 150.1001D);
			MemorySegment doubleSegmt2 = allocator.allocate(C_DOUBLE, 160.2002D);
			MemorySegment doubleSegmt3 = allocator.allocate(C_DOUBLE, 170.1001D);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromAddress(C_POINTER, doubleSegmt1.address())
					.vargFromAddress(C_POINTER, doubleSegmt2.address())
					.vargFromAddress(C_POINTER, doubleSegmt3.address()), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addDoublesByPtrFromVaList,
					FunctionDescriptor.of(C_DOUBLE, C_INT, C_POINTER), scope);

			double result = (double)mh.invoke(3, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 480.4004D, 0.0001D);
		}
	}

	@Test
	public void test_add1ByteOfStructsFromVaListByUpcallMH() throws Throwable {
		/* There are a few issues with the test on some platforms as follows:
		 * 1) VaList on Linux/x86_64 in OpenJDK is unable to handle the va_list with
		 *    over 8 arguments (confirmed by OpenJDK/Hotspot).
		 * 2) VaList on Linux/Aarch64, Windows/x86_64 and MacOS/x86_64 in OpenJDK has problem
		 *    in supporting the struct with only one integral element (confirmed by OpenJDK/Hotspot).
		 * Thus, the test is disabled on both these platforms for now till these issues
		 * are fixed in OpenJDK and verified on OpenJDK/Hotspot in the future.
		 */
		if (!isLinuxAarch64 && !isX64) {
			GroupLayout structLayout = MemoryLayout.structLayout(C_CHAR.withName("elem1"));
			VarHandle byteHandle1 = structLayout.varHandle(byte.class, PathElement.groupElement("elem1"));

			Addressable functionSymbol = nativeLibLookup.lookup("add1ByteOfStructsFromVaListByUpcallMH").get();
			MethodType mt = MethodType.methodType(byte.class, int.class, VaList.class, MemoryAddress.class);
			FunctionDescriptor fd = FunctionDescriptor.of(C_CHAR, C_INT, C_POINTER, C_POINTER);
			MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
					SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
					MemorySegment structSegmt1 = allocator.allocate(structLayout);
					byteHandle1.set(structSegmt1, (byte)1);
					MemorySegment structSegmt2 = allocator.allocate(structLayout);
					byteHandle1.set(structSegmt2, (byte)2);
					MemorySegment structSegmt3 = allocator.allocate(structLayout);
					byteHandle1.set(structSegmt3, (byte)3);
					MemorySegment structSegmt4 = allocator.allocate(structLayout);
					byteHandle1.set(structSegmt4, (byte)4);
					MemorySegment structSegmt5 = allocator.allocate(structLayout);
					byteHandle1.set(structSegmt5, (byte)5);
					MemorySegment structSegmt6 = allocator.allocate(structLayout);
					byteHandle1.set(structSegmt6, (byte)6);
					MemorySegment structSegmt7 = allocator.allocate(structLayout);
					byteHandle1.set(structSegmt7, (byte)7);
					MemorySegment structSegmt8 = allocator.allocate(structLayout);
					byteHandle1.set(structSegmt8, (byte)8);
					MemorySegment structSegmt9 = allocator.allocate(structLayout);
					byteHandle1.set(structSegmt9, (byte)9);
					MemorySegment structSegmt10 = allocator.allocate(structLayout);
					byteHandle1.set(structSegmt10, (byte)10);

					VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
							.vargFromSegment(structLayout, structSegmt2)
							.vargFromSegment(structLayout, structSegmt3)
							.vargFromSegment(structLayout, structSegmt4)
							.vargFromSegment(structLayout, structSegmt5)
							.vargFromSegment(structLayout, structSegmt6)
							.vargFromSegment(structLayout, structSegmt7)
							.vargFromSegment(structLayout, structSegmt8)
							.vargFromSegment(structLayout, structSegmt9)
							.vargFromSegment(structLayout, structSegmt10), scope);
				MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add1ByteOfStructsFromVaList,
						FunctionDescriptor.of(C_CHAR, C_INT, C_POINTER), scope);

				byte result = (byte)mh.invoke(10, vaList, upcallFuncAddr);
				Assert.assertEquals(result, 55);
			}
		}
	}

	@Test
	public void test_add2BytesOfStructsFromVaListByUpcallMH() throws Throwable {
		/* VaList on Windows/x86_64 in OpenJDK has problem in supporting the struct with
		 * two byte elements (confirmed by OpenJDK/Hotspot). Thus, the test is disabled
		 * on Windows/x86_64 for now till the issue is fixed in OpenJDK and verified on
		 * OpenJDK/Hotspot in the future.
		 */
		if (!isWinX64) {
			GroupLayout structLayout = MemoryLayout.structLayout(C_CHAR.withName("elem1"), C_CHAR.withName("elem2"));
			VarHandle byteHandle1 = structLayout.varHandle(byte.class, PathElement.groupElement("elem1"));
			VarHandle byteHandle2 = structLayout.varHandle(byte.class, PathElement.groupElement("elem2"));

			Addressable functionSymbol = nativeLibLookup.lookup("add2BytesOfStructsFromVaListByUpcallMH").get();
			MethodType mt = MethodType.methodType(byte.class, int.class, VaList.class, MemoryAddress.class);
			FunctionDescriptor fd = FunctionDescriptor.of(C_CHAR, C_INT, C_POINTER, C_POINTER);
			MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
				MemorySegment structSegmt1 = allocator.allocate(structLayout);
				byteHandle1.set(structSegmt1, (byte)1);
				byteHandle2.set(structSegmt1, (byte)2);
				MemorySegment structSegmt2 = allocator.allocate(structLayout);
				byteHandle1.set(structSegmt2, (byte)3);
				byteHandle2.set(structSegmt2, (byte)4);

				VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
						.vargFromSegment(structLayout, structSegmt2), scope);
				MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add2BytesOfStructsFromVaList,
						FunctionDescriptor.of(C_CHAR, C_INT, C_POINTER), scope);

				byte result = (byte)mh.invoke(2, vaList, upcallFuncAddr);
				Assert.assertEquals(result, 10);
			}
		}
	}

	@Test
	public void test_add3BytesOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_CHAR.withName("elem1"),
				C_CHAR.withName("elem2"), C_CHAR.withName("elem3"));
		VarHandle byteHandle1 = structLayout.varHandle(byte.class, PathElement.groupElement("elem1"));
		VarHandle byteHandle2 = structLayout.varHandle(byte.class, PathElement.groupElement("elem2"));
		VarHandle byteHandle3 = structLayout.varHandle(byte.class, PathElement.groupElement("elem3"));

		Addressable functionSymbol = nativeLibLookup.lookup("add3BytesOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(byte.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_CHAR, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			byteHandle1.set(structSegmt1, (byte)1);
			byteHandle2.set(structSegmt1, (byte)2);
			byteHandle3.set(structSegmt1, (byte)3);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			byteHandle1.set(structSegmt2, (byte)4);
			byteHandle2.set(structSegmt2, (byte)5);
			byteHandle3.set(structSegmt2, (byte)6);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add3BytesOfStructsFromVaList,
					FunctionDescriptor.of(C_CHAR, C_INT, C_POINTER), scope);

			byte result = (byte)mh.invoke(2, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 21);
		}
	}

	@Test
	public void test_add5BytesOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_CHAR.withName("elem1"),
				C_CHAR.withName("elem2"), C_CHAR.withName("elem3"), C_CHAR.withName("elem4"), C_CHAR.withName("elem5"));
		VarHandle byteHandle1 = structLayout.varHandle(byte.class, PathElement.groupElement("elem1"));
		VarHandle byteHandle2 = structLayout.varHandle(byte.class, PathElement.groupElement("elem2"));
		VarHandle byteHandle3 = structLayout.varHandle(byte.class, PathElement.groupElement("elem3"));
		VarHandle byteHandle4 = structLayout.varHandle(byte.class, PathElement.groupElement("elem4"));
		VarHandle byteHandle5 = structLayout.varHandle(byte.class, PathElement.groupElement("elem5"));

		Addressable functionSymbol = nativeLibLookup.lookup("add5BytesOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(byte.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_CHAR, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			byteHandle1.set(structSegmt1, (byte)1);
			byteHandle2.set(structSegmt1, (byte)2);
			byteHandle3.set(structSegmt1, (byte)3);
			byteHandle4.set(structSegmt1, (byte)4);
			byteHandle5.set(structSegmt1, (byte)5);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			byteHandle1.set(structSegmt2, (byte)6);
			byteHandle2.set(structSegmt2, (byte)7);
			byteHandle3.set(structSegmt2, (byte)8);
			byteHandle4.set(structSegmt2, (byte)9);
			byteHandle5.set(structSegmt2, (byte)10);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add5BytesOfStructsFromVaList,
					FunctionDescriptor.of(C_CHAR, C_INT, C_POINTER), scope);

			byte result = (byte)mh.invoke(2, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 55);
		}
	}

	@Test
	public void test_add7BytesOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_CHAR.withName("elem1"),
				C_CHAR.withName("elem2"), C_CHAR.withName("elem3"), C_CHAR.withName("elem4"),
				C_CHAR.withName("elem5"), C_CHAR.withName("elem6"), C_CHAR.withName("elem7"));
		VarHandle byteHandle1 = structLayout.varHandle(byte.class, PathElement.groupElement("elem1"));
		VarHandle byteHandle2 = structLayout.varHandle(byte.class, PathElement.groupElement("elem2"));
		VarHandle byteHandle3 = structLayout.varHandle(byte.class, PathElement.groupElement("elem3"));
		VarHandle byteHandle4 = structLayout.varHandle(byte.class, PathElement.groupElement("elem4"));
		VarHandle byteHandle5 = structLayout.varHandle(byte.class, PathElement.groupElement("elem5"));
		VarHandle byteHandle6 = structLayout.varHandle(byte.class, PathElement.groupElement("elem6"));
		VarHandle byteHandle7 = structLayout.varHandle(byte.class, PathElement.groupElement("elem7"));

		Addressable functionSymbol = nativeLibLookup.lookup("add7BytesOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(byte.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_CHAR, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			byteHandle1.set(structSegmt1, (byte)1);
			byteHandle2.set(structSegmt1, (byte)2);
			byteHandle3.set(structSegmt1, (byte)3);
			byteHandle4.set(structSegmt1, (byte)4);
			byteHandle5.set(structSegmt1, (byte)5);
			byteHandle6.set(structSegmt1, (byte)6);
			byteHandle7.set(structSegmt1, (byte)7);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			byteHandle1.set(structSegmt2, (byte)8);
			byteHandle2.set(structSegmt2, (byte)9);
			byteHandle3.set(structSegmt2, (byte)10);
			byteHandle4.set(structSegmt2, (byte)11);
			byteHandle5.set(structSegmt2, (byte)12);
			byteHandle6.set(structSegmt2, (byte)13);
			byteHandle7.set(structSegmt2, (byte)14);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add7BytesOfStructsFromVaList,
					FunctionDescriptor.of(C_CHAR, C_INT, C_POINTER), scope);

			byte result = (byte)mh.invoke(2, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 105);
		}
	}

	@Test
	public void test_add1ShortOfStructsFromVaListByUpcallMH() throws Throwable {
		/* There are a few issues with the test on some platforms as follows:
		 * 1) VaList on Linux/x86_64 in OpenJDK is unable to handle the va_list with
		 *    over 8 arguments (confirmed by OpenJDK/Hotspot).
		 * 2) VaList on Linux/Aarch64, Windows/x86_64 and MacOS/x86_64 in OpenJDK has problem
		 *    in supporting the struct with only one integral element (confirmed by OpenJDK/Hotspot).
		 * Thus, the test is disabled on both these platforms for now till these issues
		 * are fixed in OpenJDK and verified on OpenJDK/Hotspot in the future.
		 */
		if (!isLinuxAarch64 && !isX64) {
			GroupLayout structLayout = MemoryLayout.structLayout(C_SHORT.withName("elem1"));
			VarHandle shortHandle1 = structLayout.varHandle(short.class, PathElement.groupElement("elem1"));

			Addressable functionSymbol = nativeLibLookup.lookup("add1ShortOfStructsFromVaListByUpcallMH").get();
			MethodType mt = MethodType.methodType(short.class, int.class, VaList.class, MemoryAddress.class);
			FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_INT, C_POINTER, C_POINTER);
			MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
				MemorySegment structSegmt1 = allocator.allocate(structLayout);
				shortHandle1.set(structSegmt1, (short)111);
				MemorySegment structSegmt2 = allocator.allocate(structLayout);
				shortHandle1.set(structSegmt2, (short)222);
				MemorySegment structSegmt3 = allocator.allocate(structLayout);
				shortHandle1.set(structSegmt3, (short)333);
				MemorySegment structSegmt4 = allocator.allocate(structLayout);
				shortHandle1.set(structSegmt4, (short)444);
				MemorySegment structSegmt5 = allocator.allocate(structLayout);
				shortHandle1.set(structSegmt5, (short)555);
				MemorySegment structSegmt6 = allocator.allocate(structLayout);
				shortHandle1.set(structSegmt6, (short)666);
				MemorySegment structSegmt7 = allocator.allocate(structLayout);
				shortHandle1.set(structSegmt7, (short)777);
				MemorySegment structSegmt8 = allocator.allocate(structLayout);
				shortHandle1.set(structSegmt8, (short)888);
				MemorySegment structSegmt9 = allocator.allocate(structLayout);
				shortHandle1.set(structSegmt9, (short)999);
				MemorySegment structSegmt10 = allocator.allocate(structLayout);
				shortHandle1.set(structSegmt10, (short)123);

				VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
						.vargFromSegment(structLayout, structSegmt2)
						.vargFromSegment(structLayout, structSegmt3)
						.vargFromSegment(structLayout, structSegmt4)
						.vargFromSegment(structLayout, structSegmt5)
						.vargFromSegment(structLayout, structSegmt6)
						.vargFromSegment(structLayout, structSegmt7)
						.vargFromSegment(structLayout, structSegmt8)
						.vargFromSegment(structLayout, structSegmt9)
						.vargFromSegment(structLayout, structSegmt10), scope);
				MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add1ShortOfStructsFromVaList,
						FunctionDescriptor.of(C_SHORT, C_INT, C_POINTER), scope);

				short result = (short)mh.invoke(10, vaList, upcallFuncAddr);
				Assert.assertEquals(result, 5118);
			}
		}
	}

	@Test
	public void test_add2ShortsOfStructsFromVaListByUpcallMH() throws Throwable {
		/* VaList on Windows/x86_64 in OpenJDK has problem in supporting the struct with
		 * two short elements (confirmed by OpenJDK/Hotspot). Thus, the test is disabled
		 * on Windows/x86_64 for now till the issue is fixed in OpenJDK and verified on
		 * OpenJDK/Hotspot in the future.
		 */
		if (!isWinX64) {
			GroupLayout structLayout = MemoryLayout.structLayout(C_SHORT.withName("elem1"), C_SHORT.withName("elem2"));
			VarHandle shortHandle1 = structLayout.varHandle(short.class, PathElement.groupElement("elem1"));
			VarHandle shortHandle2 = structLayout.varHandle(short.class, PathElement.groupElement("elem2"));

			Addressable functionSymbol = nativeLibLookup.lookup("add2ShortsOfStructsFromVaListByUpcallMH").get();
			MethodType mt = MethodType.methodType(short.class, int.class, VaList.class, MemoryAddress.class);
			FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_INT, C_POINTER, C_POINTER);
			MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
				MemorySegment structSegmt1 = allocator.allocate(structLayout);
				shortHandle1.set(structSegmt1, (short)111);
				shortHandle2.set(structSegmt1, (short)222);
				MemorySegment structSegmt2 = allocator.allocate(structLayout);
				shortHandle1.set(structSegmt2, (short)333);
				shortHandle2.set(structSegmt2, (short)444);

				VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
						.vargFromSegment(structLayout, structSegmt2), scope);
				MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add2ShortsOfStructsFromVaList,
						FunctionDescriptor.of(C_SHORT, C_INT, C_POINTER), scope);

				short result = (short)mh.invoke(2, vaList, upcallFuncAddr);
				Assert.assertEquals(result, 1110);
			}
		}
	}

	@Test
	public void test_add3ShortsOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_SHORT.withName("elem1"),
				C_SHORT.withName("elem2"), C_SHORT.withName("elem3"));
		VarHandle shortHandle1 = structLayout.varHandle(short.class, PathElement.groupElement("elem1"));
		VarHandle shortHandle2 = structLayout.varHandle(short.class, PathElement.groupElement("elem2"));
		VarHandle shortHandle3 = structLayout.varHandle(short.class, PathElement.groupElement("elem3"));

		Addressable functionSymbol = nativeLibLookup.lookup("add3ShortsOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(short.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_SHORT, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			shortHandle1.set(structSegmt1, (short)111);
			shortHandle2.set(structSegmt1, (short)222);
			shortHandle3.set(structSegmt1, (short)333);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			shortHandle1.set(structSegmt2, (short)444);
			shortHandle2.set(structSegmt2, (short)555);
			shortHandle3.set(structSegmt2, (short)666);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add3ShortsOfStructsFromVaList,
					FunctionDescriptor.of(C_SHORT, C_INT, C_POINTER), scope);

			short result = (short)mh.invoke(2, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 2331);
		}
	}

	@Test
	public void test_add1IntOfStructsFromVaListByUpcallMH() throws Throwable {
		/* There are a few issues with the test on some platforms as follows:
		 * 1) VaList on Linux/x86_64 in OpenJDK is unable to handle the va_list with
		 *    over 8 arguments (confirmed by OpenJDK/Hotspot).
		 * 2) VaList on Linux/Aarch64, Windows/x86_64 and MacOS/x86_64 in OpenJDK has problem
		 *    in supporting the struct with only one integral element (confirmed by OpenJDK/Hotspot).
		 * Thus, the test is disabled on both these platforms for now till these issues
		 * are fixed in OpenJDK and verified on OpenJDK/Hotspot in the future.
		 */
		if (!isLinuxAarch64 && !isX64) {
			GroupLayout structLayout = MemoryLayout.structLayout(C_INT.withName("elem1"));
			VarHandle intHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));

			Addressable functionSymbol = nativeLibLookup.lookup("add1IntOfStructsFromVaListByUpcallMH").get();
			MethodType mt = MethodType.methodType(int.class, int.class, VaList.class, MemoryAddress.class);
			FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_POINTER, C_POINTER);
			MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
				MemorySegment structSegmt1 = allocator.allocate(structLayout);
				intHandle1.set(structSegmt1, 1111111);
				MemorySegment structSegmt2 = allocator.allocate(structLayout);
				intHandle1.set(structSegmt2, 2222222);
				MemorySegment structSegmt3 = allocator.allocate(structLayout);
				intHandle1.set(structSegmt3, 3333333);
				MemorySegment structSegmt4 = allocator.allocate(structLayout);
				intHandle1.set(structSegmt4, 4444444);
				MemorySegment structSegmt5 = allocator.allocate(structLayout);
				intHandle1.set(structSegmt5, 5555555);
				MemorySegment structSegmt6 = allocator.allocate(structLayout);
				intHandle1.set(structSegmt6, 6666666);
				MemorySegment structSegmt7 = allocator.allocate(structLayout);
				intHandle1.set(structSegmt7, 7777777);
				MemorySegment structSegmt8 = allocator.allocate(structLayout);
				intHandle1.set(structSegmt8, 8888888);
				MemorySegment structSegmt9 = allocator.allocate(structLayout);
				intHandle1.set(structSegmt9, 9999999);
				MemorySegment structSegmt10 = allocator.allocate(structLayout);
				intHandle1.set(structSegmt10, 1234567);

				VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
						.vargFromSegment(structLayout, structSegmt2)
						.vargFromSegment(structLayout, structSegmt3)
						.vargFromSegment(structLayout, structSegmt4)
						.vargFromSegment(structLayout, structSegmt5)
						.vargFromSegment(structLayout, structSegmt6)
						.vargFromSegment(structLayout, structSegmt7)
						.vargFromSegment(structLayout, structSegmt8)
						.vargFromSegment(structLayout, structSegmt9)
						.vargFromSegment(structLayout, structSegmt10), scope);
				MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add1IntOfStructsFromVaList,
						FunctionDescriptor.of(C_INT, C_INT, C_POINTER), scope);

				int result = (int)mh.invoke(10, vaList, upcallFuncAddr);
				Assert.assertEquals(result, 51234562);
			}
		}
	}

	@Test
	public void test_add2IntsOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_INT.withName("elem1"), C_INT.withName("elem2"));
		VarHandle intHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle intHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("add2IntsOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(int.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			intHandle1.set(structSegmt1, 1122333);
			intHandle2.set(structSegmt1, 4455666);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			intHandle1.set(structSegmt2, 2244668);
			intHandle2.set(structSegmt2, 1133557);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add2IntsOfStructsFromVaList,
					FunctionDescriptor.of(C_INT, C_INT, C_POINTER), scope);

			int result = (int)mh.invoke(2, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 8956224);
		}
	}

	@Test
	public void test_add3IntsOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_INT.withName("elem1"),
				C_INT.withName("elem2"), C_INT.withName("elem3"));
		VarHandle intHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle intHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));
		VarHandle intHandle3 = structLayout.varHandle(int.class, PathElement.groupElement("elem3"));

		Addressable functionSymbol = nativeLibLookup.lookup("add3IntsOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(int.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			intHandle1.set(structSegmt1, 1122333);
			intHandle2.set(structSegmt1, 4455666);
			intHandle3.set(structSegmt1, 7788999);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			intHandle1.set(structSegmt2, 1133555);
			intHandle2.set(structSegmt2, 2244666);
			intHandle3.set(structSegmt2, 3322111);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add3IntsOfStructsFromVaList,
					FunctionDescriptor.of(C_INT, C_INT, C_POINTER), scope);

			int result = (int)mh.invoke(2, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 20067330);
		}
	}

	@Test
	public void test_add2LongsOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(longLayout.withName("elem1"), longLayout.withName("elem2"));
		VarHandle longHandle1 = structLayout.varHandle(long.class, PathElement.groupElement("elem1"));
		VarHandle longHandle2 = structLayout.varHandle(long.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("add2LongsOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(long.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			longHandle1.set(structSegmt1, 1122334455L);
			longHandle2.set(structSegmt1, 6677889911L);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			longHandle1.set(structSegmt2, 2233445566L);
			longHandle2.set(structSegmt2, 7788991122L);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add2LongsOfStructsFromVaList,
					FunctionDescriptor.of(longLayout, C_INT, C_POINTER), scope);

			long result = (long)mh.invoke(2, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 17822661054L);
		}
	}

	@Test
	public void test_add1FloatOfStructsFromVaListByUpcallMH() throws Throwable {
		/* VaList on Windows/x86_64 in OpenJDK has problem in supporting the struct with only
		 * one float element (confirmed by OpenJDK/Hotspot). Thus, the test is disabled on
		 * Windows/x86_64 for now till the issue is fixed in OpenJDK and verified on
		 * OpenJDK/Hotspot in the future.
		 */
		if (!isWinX64) {
			GroupLayout structLayout = MemoryLayout.structLayout(C_FLOAT.withName("elem1"));
			VarHandle floatHandle1 = structLayout.varHandle(float.class, PathElement.groupElement("elem1"));

			Addressable functionSymbol = nativeLibLookup.lookup("add1FloatOfStructsFromVaListByUpcallMH").get();
			MethodType mt = MethodType.methodType(float.class, int.class, VaList.class, MemoryAddress.class);
			FunctionDescriptor fd = FunctionDescriptor.of(C_FLOAT, C_INT, C_POINTER, C_POINTER);
			MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
				MemorySegment structSegmt1 = allocator.allocate(structLayout);
				floatHandle1.set(structSegmt1, 1.11F);
				MemorySegment structSegmt2 = allocator.allocate(structLayout);
				floatHandle1.set(structSegmt2, 2.22F);
				MemorySegment structSegmt3 = allocator.allocate(structLayout);
				floatHandle1.set(structSegmt3, 3.33F);
				MemorySegment structSegmt4 = allocator.allocate(structLayout);
				floatHandle1.set(structSegmt4, 4.44F);
				MemorySegment structSegmt5 = allocator.allocate(structLayout);
				floatHandle1.set(structSegmt5, 5.56F);

				VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
						.vargFromSegment(structLayout, structSegmt2)
						.vargFromSegment(structLayout, structSegmt3)
						.vargFromSegment(structLayout, structSegmt4)
						.vargFromSegment(structLayout, structSegmt5), scope);
				MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add1FloatOfStructsFromVaList,
						FunctionDescriptor.of(C_FLOAT, C_INT, C_POINTER), scope);

				float result = (float)mh.invoke(5, vaList, upcallFuncAddr);
				Assert.assertEquals(result, 16.66F, 0.01F);
			}
		}
	}

	@Test
	public void test_add2FloatsOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_FLOAT.withName("elem1"), C_FLOAT.withName("elem2"));
		VarHandle floatHandle1 = structLayout.varHandle(float.class, PathElement.groupElement("elem1"));
		VarHandle floatHandle2 = structLayout.varHandle(float.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("add2FloatsOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(float.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_FLOAT, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			floatHandle1.set(structSegmt1, 1.11F);
			floatHandle2.set(structSegmt1, 2.22F);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			floatHandle1.set(structSegmt2, 3.33F);
			floatHandle2.set(structSegmt2, 4.44F);
			MemorySegment structSegmt3 = allocator.allocate(structLayout);
			floatHandle1.set(structSegmt3, 5.55F);
			floatHandle2.set(structSegmt3, 6.66F);
			MemorySegment structSegmt4 = allocator.allocate(structLayout);
			floatHandle1.set(structSegmt4, 7.77F);
			floatHandle2.set(structSegmt4, 8.88F);
			MemorySegment structSegmt5 = allocator.allocate(structLayout);
			floatHandle1.set(structSegmt5, 9.99F);
			floatHandle2.set(structSegmt5, 1.23F);
			MemorySegment structSegmt6 = allocator.allocate(structLayout);
			floatHandle1.set(structSegmt6, 4.56F);
			floatHandle2.set(structSegmt6, 7.89F);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2)
					.vargFromSegment(structLayout, structSegmt3)
					.vargFromSegment(structLayout, structSegmt4)
					.vargFromSegment(structLayout, structSegmt5)
					.vargFromSegment(structLayout, structSegmt6), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add2FloatsOfStructsFromVaList,
					FunctionDescriptor.of(C_FLOAT, C_INT, C_POINTER), scope);

			float result = (float)mh.invoke(6, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 63.63F, 0.01F);
		}
	}

	@Test
	public void test_add3FloatsOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = isStructPaddingNotRequired ? MemoryLayout.structLayout(C_FLOAT.withName("elem1"),
				C_FLOAT.withName("elem2"), C_FLOAT.withName("elem3")) : MemoryLayout.structLayout(C_FLOAT.withName("elem1"),
						C_FLOAT.withName("elem2"), C_FLOAT.withName("elem3"), MemoryLayout.paddingLayout(32));
		VarHandle floatHandle1 = structLayout.varHandle(float.class, PathElement.groupElement("elem1"));
		VarHandle floatHandle2 = structLayout.varHandle(float.class, PathElement.groupElement("elem2"));
		VarHandle floatHandle3 = structLayout.varHandle(float.class, PathElement.groupElement("elem3"));

		Addressable functionSymbol = nativeLibLookup.lookup("add3FloatsOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(float.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_FLOAT, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			floatHandle1.set(structSegmt1, 1.11F);
			floatHandle2.set(structSegmt1, 2.22F);
			floatHandle3.set(structSegmt1, 3.33F);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			floatHandle1.set(structSegmt2, 4.44F);
			floatHandle2.set(structSegmt2, 5.55F);
			floatHandle3.set(structSegmt2, 6.66F);
			MemorySegment structSegmt3 = allocator.allocate(structLayout);
			floatHandle1.set(structSegmt3, 7.77F);
			floatHandle2.set(structSegmt3, 8.88F);
			floatHandle3.set(structSegmt3, 9.99F);
			MemorySegment structSegmt4 = allocator.allocate(structLayout);
			floatHandle1.set(structSegmt4, 1.23F);
			floatHandle2.set(structSegmt4, 4.56F);
			floatHandle3.set(structSegmt4, 7.89F);
			MemorySegment structSegmt5 = allocator.allocate(structLayout);
			floatHandle1.set(structSegmt5, 9.87F);
			floatHandle2.set(structSegmt5, 6.54F);
			floatHandle3.set(structSegmt5, 3.21F);
			MemorySegment structSegmt6 = allocator.allocate(structLayout);
			floatHandle1.set(structSegmt6, 2.46F);
			floatHandle2.set(structSegmt6, 8.13F);
			floatHandle3.set(structSegmt6, 5.79F);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2)
					.vargFromSegment(structLayout, structSegmt3)
					.vargFromSegment(structLayout, structSegmt4)
					.vargFromSegment(structLayout, structSegmt5)
					.vargFromSegment(structLayout, structSegmt6), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add3FloatsOfStructsFromVaList,
					FunctionDescriptor.of(C_FLOAT, C_INT, C_POINTER), scope);

			float result = (float)mh.invoke(6, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 99.63F, 0.01F);
		}
	}

	@Test
	public void test_add1DoubleOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_DOUBLE.withName("elem1"));
		VarHandle doubleHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));

		Addressable functionSymbol = nativeLibLookup.lookup("add1DoubleOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(double.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			doubleHandle1.set(structSegmt1, 11111.1001D);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			doubleHandle1.set(structSegmt2, 11111.1002D);
			MemorySegment structSegmt3 = allocator.allocate(structLayout);
			doubleHandle1.set(structSegmt3, 11111.1003D);
			MemorySegment structSegmt4 = allocator.allocate(structLayout);
			doubleHandle1.set(structSegmt4, 11111.1004D);
			MemorySegment structSegmt5 = allocator.allocate(structLayout);
			doubleHandle1.set(structSegmt5, 11111.1005D);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2)
					.vargFromSegment(structLayout, structSegmt3)
					.vargFromSegment(structLayout, structSegmt4)
					.vargFromSegment(structLayout, structSegmt5), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add1DoubleOfStructsFromVaList,
					FunctionDescriptor.of(C_DOUBLE, C_INT, C_POINTER), scope);

			double result = (double)mh.invoke(5, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 55555.5015D, 0.0001D);
		}
	}

	@Test
	public void test_add2DoublesOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_DOUBLE.withName("elem1"), C_DOUBLE.withName("elem2"));
		VarHandle doubleHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));
		VarHandle doubleHandle2 = structLayout.varHandle(double.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("add2DoublesOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(double.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			doubleHandle1.set(structSegmt1, 11150.1001D);
			doubleHandle2.set(structSegmt1, 11160.2002D);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			doubleHandle1.set(structSegmt2, 11170.1001D);
			doubleHandle2.set(structSegmt2, 11180.2002D);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_add2DoublesOfStructsFromVaList,
					FunctionDescriptor.of(C_DOUBLE, C_INT, C_POINTER), scope);

			double result = (double)mh.invoke(2, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 44660.6006D, 0.0001D);
		}
	}

	@Test
	public void test_addIntShortOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_INT.withName("elem1"), C_SHORT.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(short.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("addIntShortOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(int.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt1, 1111111);
			elemHandle2.set(structSegmt1, (short)123);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt2, 2222222);
			elemHandle2.set(structSegmt2, (short)456);
			MemorySegment structSegmt3 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt3, 3333333);
			elemHandle2.set(structSegmt3, (short)789);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2)
					.vargFromSegment(structLayout, structSegmt3), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addIntShortOfStructsFromVaList,
					FunctionDescriptor.of(C_INT, C_INT, C_POINTER), scope);

			int result = (int)mh.invoke(3, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 6668034);
		}
	}

	@Test
	public void test_addShortIntOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_SHORT.withName("elem1"),
				MemoryLayout.paddingLayout(16), C_INT.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(short.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("addShortIntOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(int.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt1, (short)123);
			elemHandle2.set(structSegmt1, 1111111);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt2, (short)456);
			elemHandle2.set(structSegmt2, 2222222);
			MemorySegment structSegmt3 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt3, (short)789);
			elemHandle2.set(structSegmt3, 3333333);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2)
					.vargFromSegment(structLayout, structSegmt3), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addShortIntOfStructsFromVaList,
					FunctionDescriptor.of(C_INT, C_INT, C_POINTER), scope);

			int result = (int)mh.invoke(3, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 6668034);
		}
	}

	@Test
	public void test_addIntLongOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_INT.withName("elem1"),
				MemoryLayout.paddingLayout(32), longLayout.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(int.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(long.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("addIntLongOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(long.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt1, 1111111);
			elemHandle2.set(structSegmt1, 101010101010L);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt2, 2222222);
			elemHandle2.set(structSegmt2, 202020202020L);
			MemorySegment structSegmt3 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt3, 3333333);
			elemHandle2.set(structSegmt3, 303030303030L);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2)
					.vargFromSegment(structLayout, structSegmt3), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addIntLongOfStructsFromVaList,
					FunctionDescriptor.of(longLayout, C_INT, C_POINTER), scope);

			long result = (long)mh.invoke(3, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 606067272726L);
		}
	}

	@Test
	public void test_addLongIntOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(longLayout.withName("elem1"),
				C_INT.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(long.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(int.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("addLongIntOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(long.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(longLayout, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt1, 101010101010L);
			elemHandle2.set(structSegmt1, 1111111);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt2, 202020202020L);
			elemHandle2.set(structSegmt2, 2222222);
			MemorySegment structSegmt3 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt3, 303030303030L);
			elemHandle2.set(structSegmt3, 3333333);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2)
					.vargFromSegment(structLayout, structSegmt3), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addLongIntOfStructsFromVaList,
					FunctionDescriptor.of(longLayout, C_INT, C_POINTER), scope);

			long result = (long)mh.invoke(3, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 606067272726L);
		}
	}

	@Test
	public void test_addFloatDoubleOfStructsFromVaListByUpcallMH() throws Throwable {
		/* The size of [float, double] on AIX/PPC 64-bit is 12 bytes without padding by default
		 * while the same struct is 16 bytes with padding on other platforms.
		 */
		GroupLayout structLayout = isAixOS ? MemoryLayout.structLayout(C_FLOAT.withName("elem1"),
				C_DOUBLE.withName("elem2")) : MemoryLayout.structLayout(C_FLOAT.withName("elem1"),
				MemoryLayout.paddingLayout(32), C_DOUBLE.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(float.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(double.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("addFloatDoubleOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(double.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt1, 1.11F);
			elemHandle2.set(structSegmt1, 222.222D);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt2, 2.22F);
			elemHandle2.set(structSegmt2, 333.333D);
			MemorySegment structSegmt3 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt3, 3.33F);
			elemHandle2.set(structSegmt3, 444.444D);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2)
					.vargFromSegment(structLayout, structSegmt3), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addFloatDoubleOfStructsFromVaList,
					FunctionDescriptor.of(C_DOUBLE, C_INT, C_POINTER), scope);

			double result = (double)mh.invoke(3, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 1006.659D, 0.001D);
		}
	}

	@Test
	public void test_addDoubleFloatOfStructsFromVaListByUpcallMH() throws Throwable {
		GroupLayout structLayout = MemoryLayout.structLayout(C_DOUBLE.withName("elem1"), C_FLOAT.withName("elem2"));
		VarHandle elemHandle1 = structLayout.varHandle(double.class, PathElement.groupElement("elem1"));
		VarHandle elemHandle2 = structLayout.varHandle(float.class, PathElement.groupElement("elem2"));

		Addressable functionSymbol = nativeLibLookup.lookup("addDoubleFloatOfStructsFromVaListByUpcallMH").get();
		MethodType mt = MethodType.methodType(double.class, int.class, VaList.class, MemoryAddress.class);
		FunctionDescriptor fd = FunctionDescriptor.of(C_DOUBLE, C_INT, C_POINTER, C_POINTER);
		MethodHandle mh = clinker.downcallHandle(functionSymbol, mt, fd);

		try (ResourceScope scope = ResourceScope.newConfinedScope()) {
			SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
			MemorySegment structSegmt1 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt1, 222.222D);
			elemHandle2.set(structSegmt1, 1.11F);
			MemorySegment structSegmt2 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt2, 333.333D);
			elemHandle2.set(structSegmt2, 2.22F);
			MemorySegment structSegmt3 = allocator.allocate(structLayout);
			elemHandle1.set(structSegmt3, 444.444D);
			elemHandle2.set(structSegmt3, 3.33F);

			VaList vaList = VaList.make(vaListBuilder -> vaListBuilder.vargFromSegment(structLayout, structSegmt1)
					.vargFromSegment(structLayout, structSegmt2)
					.vargFromSegment(structLayout, structSegmt3), scope);
			MemoryAddress upcallFuncAddr = clinker.upcallStub(VaListUpcallMethodHandles.MH_addDoubleFloatOfStructsFromVaList,
					FunctionDescriptor.of(C_DOUBLE, C_INT, C_POINTER), scope);

			double result = (double)mh.invoke(3, vaList, upcallFuncAddr);
			Assert.assertEquals(result, 1006.659D, 0.001D);
		}
	}
}
