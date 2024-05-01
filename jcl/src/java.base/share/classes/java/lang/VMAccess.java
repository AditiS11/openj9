/*[INCLUDE-IF Sidecar16]*/
/*
 * Copyright IBM Corp. and others 2012
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

package java.lang;

import java.util.Objects;
import java.util.Properties;

/*[IF JAVA_SPEC_VERSION >= 9]*/
import jdk.internal.reflect.ConstantPool;
/*[ELSE] JAVA_SPEC_VERSION >= 9 */
import sun.reflect.ConstantPool;
/*[ENDIF] JAVA_SPEC_VERSION >= 9 */

import com.ibm.oti.vm.*;
import com.ibm.jit.JITHelpers;

/**
 * Helper class to allow privileged access to classes
 * from outside the java.lang package. Based on sun.misc.SharedSecrets
 * implementation.
 */
final class VMAccess implements VMLangAccess {

	private static ClassLoader extClassLoader;

	/**
	 * Native used to find and load a class using the VM
	 *
	 * @return 		java.lang.Class
	 *					the class or null.
	 * @param 		className String
	 *					the name of the class to search for.
	 * @param		classLoader
	 *					the classloader to do the work
	 */
	static native Class<?> findClassOrNull(String className, ClassLoader classLoader);

	@Override
	public Class<?> findClassOrNullHelper(String className, ClassLoader classLoader) {
		return VMAccess.findClassOrNull(className, classLoader);
	}


/*[IF JAVA_SPEC_VERSION >= 9]*/
	/**
	 * Answer the platform class loader.
	 */
	@Override
	public ClassLoader getPlatformClassLoader() {
		return jdk.internal.loader.ClassLoaders.platformClassLoader();
	}
/*[ENDIF] JAVA_SPEC_VERSION >= 9 */

	/**
	 * Set the extension class loader. It can only be set once.
	 *
	 * @param loader the extension class loader
	 */
	static void setExtClassLoader(ClassLoader loader) {
		extClassLoader = loader;
	}

	/**
	 * Answer the extension class loader.
	 */
	@Override
	public ClassLoader getExtClassLoader() {
		return extClassLoader;
	}

	/**
	 * Returns true if parent is the ancestor of child.
	 * Parent and child must not be null.
	 */
	/*[PR CMVC 191554] Provide access to ClassLoader methods to improve performance */
	@Override
	public boolean isAncestor(java.lang.ClassLoader parent, java.lang.ClassLoader child) {
		return parent.isAncestorOf(child);
	}

	/**
	 * Returns the ClassLoader off clazz.
	 */
	/*[PR CMVC 191554] Provide access to ClassLoader methods to improve performance */
	@Override
	public java.lang.ClassLoader getClassloader(java.lang.Class clazz) {
		return clazz.getClassLoaderImpl();
	}

	/**
	 * Returns the package name for a given class.
	 * clazz must not be null.
	 */
	/*[PR CMVC 191554] Provide access to ClassLoader methods to improve performance */
	@Override
	public java.lang.String getPackageName(java.lang.Class clazz) {
		return clazz.getPackageName();
	}

	/**
	 * Returns a MethodHandle cache for a given class.
	 */
	@Override
	public java.lang.Object getMethodHandleCache(java.lang.Class<?> clazz) {
		return clazz.getMethodHandleCache();
	}

	/**
	 * Set a MethodHandle cache to a given class.
	 */
	@Override
	public java.lang.Object setMethodHandleCache(java.lang.Class<?> clazz, java.lang.Object object) {
		return clazz.setMethodHandleCache(object);
	}

	/**
	 * Returns a {@code java.util.Map} from method descriptor string to the equivalent {@code MethodType} as generated by {@code MethodType.fromMethodDescriptorString}.
	 * @param loader The {@code ClassLoader} used to get the MethodType.
	 * @return A {@code java.util.Map} from method descriptor string to the equivalent {@code MethodType}.
	 */
	@Override
	public java.util.Map<String, java.lang.invoke.MethodType> getMethodTypeCache(ClassLoader loader) {
		return loader != null ? loader.getMethodTypeCache() : null;
	}

	/**
	 *	Provide internal access to the system properties without going through SecurityManager
	 *
	 *  Important notes:
	 *  	1. This API must NOT be exposed to application code directly or indirectly;
	 *  	2. This method can only be used to retrieve system properties for internal usage,
	 *  		i.e., there is no security exception expected;
	 *  	3. If there is an application caller in the call stack, AND the application caller(s)
	 *  		have to be check for permission to retrieve the system properties specified,
	 *  		then this API should NOT be used even though the immediate caller is in boot strap path.
	 *
	 * @return the system properties
	 */
	@Override
	public Properties internalGetProperties() {
		return System.internalGetProperties();
	}

	/*[IF JAVA_SPEC_VERSION == 8]*/
	/**
	 * Returns the system packages for the bootloader
	 * @return An array of packages defined by the bootloader
	 */
	@Override
	public Package[] getSystemPackages() {
		return Package.getSystemPackages();
	}

	/**
	 * Returns the system package for the 'name'
	 * @param name must not be null
	 * @return The package
	 */
	@Override
	public Package getSystemPackage(String name) {
		return Package.getSystemPackage(name);
	}
	/*[ENDIF] JAVA_SPEC_VERSION == 8 */

	/**
	 * Returns an InternalConstantPool object.
	 *
	 * @param addr - the native addr of the J9ConstantPool
	 * @return An InternalConstantPool reference object
	 */
	@Override
	public Object createInternalConstantPool(long addr) {
		return new InternalConstantPool(addr);
	}

	/**
	 * Returns a ConstantPool object
	 * @param internalConstantPool An object ref to a j9constantpool
	 * @return ConstantPool instance
	 */
	@Override
	public ConstantPool getConstantPool(Object internalConstantPool) {
		return Access.getConstantPool(internalConstantPool);
	}

	/**
	 * Returns an InternalConstantPool object from a J9Class address. The ConstantPool
	 * natives expect an InternalConstantPool as the constantPoolOop parameter.
	 *
	 * @param j9class the native address of the J9Class
	 * @return InternalConstantPool a wrapper for a j9constantpool
	 */
	public Object getInternalConstantPoolFromJ9Class(long j9class) {
		long j9constantpool = VM.getJ9ConstantPoolFromJ9Class(j9class);
		return createInternalConstantPool(j9constantpool);
	}

	/**
	 * Returns an InternalConstantPool object from a Class. The ConstantPool
	 * natives expect an InternalConstantPool as the constantPoolOop parameter.
	 *
	 * @param clazz the Class to fetch the constant pool from
	 * @return an InternalConstantPool wrapper for a j9constantpool
	 */
	public Object getInternalConstantPoolFromClass(Class clazz) {
		JITHelpers helpers = JITHelpers.getHelpers();
		long j9class;
		if (helpers.is32Bit()) {
			j9class = helpers.getJ9ClassFromClass32(clazz);
		} else {
			j9class = helpers.getJ9ClassFromClass64(clazz);
		}
		return getInternalConstantPoolFromJ9Class(j9class);
	}

	/*[IF JAVA_SPEC_VERSION >= 9]*/
	@Override
	public void addPackageToList(java.lang.Class<?> newClass, ClassLoader loader) {
		java.lang.ClassLoader packageLoader = loader;
		if (Objects.isNull(packageLoader)) {
			packageLoader = ClassLoader.getSystemClassLoader();
		}
		packageLoader.addPackageToList(newClass);
	}
	/*[ENDIF] JAVA_SPEC_VERSION >= 9 */

	@Override
	public Thread createThread(Runnable runnable, String threadName, boolean isSystemThreadGroup, boolean inheritThreadLocals, boolean isDaemon, ClassLoader contextClassLoader) {
		return new Thread(runnable, threadName, isSystemThreadGroup, inheritThreadLocals, isDaemon, contextClassLoader);
	}

	@Override
	public void prepare(Class<?> theClass) {
		J9VMInternals.prepare(theClass);
	}

	/*[IF JAVA_SPEC_VERSION >= 11]*/
	/**
	 * Returns whether the classloader name should be included in the stack trace for the provided StackTraceElement.
	 *
	 * @param element The StackTraceElement to check
	 * @return true if the classloader name should be included, false otherwise
	 */
	@Override
	public boolean getIncludeClassLoaderName(StackTraceElement element) {
		return element.getIncludeClassLoaderName();
	}

	/**
	 * Returns whether the module version should be included in the stack trace for the provided StackTraceElement.
	 *
	 * @param element The StackTraceElement to check
	 * @return true if the module version should be included, false otherwise
	 */
	@Override
	public boolean getIncludeModuleVersion(StackTraceElement element) {
		return element.getIncludeModuleVersion();
	}
	/*[ENDIF] JAVA_SPEC_VERSION >= 11 */
}
