/*[INCLUDE-IF (JAVA_SPEC_VERSION >= 9) & !OPENJDK_METHODHANDLES]*/
/*
 * Copyright IBM Corp. and others 2018
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
package jdk.tools.jlink.internal.plugins;

import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import java.util.EnumSet;
import java.util.Set;

/**
 * We override the default GenerateJLIClassesPlugin with this
 * one because the default relies on Hotspot-specific code.
 * Removing this class entirely causes the build to fail when
 * using the default module-info.java file for the jlink module.
 * So we create the class, but include no code that affects any
 * change.
 */
public class GenerateJLIClassesPlugin implements Plugin
{
	private static final String NAME = "generate-jli-classes"; //$NON-NLS-1$

	/**
	 * Returns the plugin name so that jlink can match both
	 * --generate-jli-classes=@... and --disable-plugin generate-jli-classes.
	 * @return String plugin name.
	 */
	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * Pass-through transform: copies every entry from the input pool to the
	 * output unchanged. This stub intentionally performs no JLI class
	 * generation because the real implementation depends on HotSpot internals
	 * that are not present in OpenJ9.
	 * @param rp  Pool of resources
	 * @param rpb Builder to create a copy of the pool of resources.
	 * @return ResourcePool Built copy of the pool of resources.
	 */
	@Override
	public ResourcePool transform(ResourcePool rp, ResourcePoolBuilder rpb) {
		rp.transformAndCopy(entry -> entry, rpb);
		return rpb.build();
	}

	/**
	 * Declare the plugin as AUTO_ENABLED and FUNCTIONAL so that jlink
	 * registers the --generate-jli-classes option and accepts arguments
	 * for it. Without AUTO_ENABLED the option is never registered and
	 * passing --generate-jli-classes=@... causes "unknown option".
	 * @return Set<State> State of the Plugin.
	 */
	@Override
	public Set<State> getState() {
		return EnumSet.of(State.AUTO_ENABLED, State.FUNCTIONAL);
	}

	/**
	 * Declare that this plugin accepts an argument (the @tracefile path)
	 * so that jlink registers the option as --generate-jli-classes=<arg>.
	 * @return true
	 */
	@Override
	public boolean hasArguments() {
		return true;
	}
}
