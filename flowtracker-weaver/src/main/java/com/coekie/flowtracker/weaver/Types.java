package com.coekie.flowtracker.weaver;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.objectweb.asm.Type;

public class Types {
	public static final Type CHAR_ARRAY = Type.getType("[C");
	public static final Type BYTE_ARRAY = Type.getType("[B");
	public static final Type OBJECT = Type.getObjectType("java/lang/Object");
	public static final Type STRING = Type.getObjectType("java/lang/String");
	public static final Type INVOCATION =
			Type.getObjectType("com/coekie/flowtracker/tracker/Invocation");
}
