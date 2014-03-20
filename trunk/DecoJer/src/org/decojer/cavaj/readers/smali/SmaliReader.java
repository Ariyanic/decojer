/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  André Pankraz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every Java Source Code
 * that is created using DecoJer.
 */
package org.decojer.cavaj.readers.smali;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.fields.F;
import org.decojer.cavaj.model.methods.M;
import org.decojer.cavaj.model.types.ClassT;
import org.decojer.cavaj.model.types.T;
import org.decojer.cavaj.readers.DexReader;
import org.jf.dexlib.AnnotationDirectoryItem;
import org.jf.dexlib.AnnotationDirectoryItem.FieldAnnotation;
import org.jf.dexlib.AnnotationDirectoryItem.MethodAnnotation;
import org.jf.dexlib.AnnotationDirectoryItem.ParameterAnnotation;
import org.jf.dexlib.AnnotationItem;
import org.jf.dexlib.AnnotationSetItem;
import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDataItem.EncodedField;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.EncodedArrayItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.ItemType;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.Section;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.TypeListItem;
import org.jf.dexlib.EncodedValue.AnnotationEncodedSubValue;
import org.jf.dexlib.EncodedValue.AnnotationEncodedValue;
import org.jf.dexlib.EncodedValue.ArrayEncodedValue;
import org.jf.dexlib.EncodedValue.BooleanEncodedValue;
import org.jf.dexlib.EncodedValue.ByteEncodedValue;
import org.jf.dexlib.EncodedValue.CharEncodedValue;
import org.jf.dexlib.EncodedValue.DoubleEncodedValue;
import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.EncodedValue.EnumEncodedValue;
import org.jf.dexlib.EncodedValue.FloatEncodedValue;
import org.jf.dexlib.EncodedValue.IntEncodedValue;
import org.jf.dexlib.EncodedValue.LongEncodedValue;
import org.jf.dexlib.EncodedValue.MethodEncodedValue;
import org.jf.dexlib.EncodedValue.NullEncodedValue;
import org.jf.dexlib.EncodedValue.ShortEncodedValue;
import org.jf.dexlib.EncodedValue.StringEncodedValue;
import org.jf.dexlib.EncodedValue.TypeEncodedValue;
import org.jf.dexlib.Util.ByteArrayInput;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;

/**
 * Reader from Smali.
 *
 * @author André Pankraz
 */
@Slf4j
public class SmaliReader implements DexReader {

	private final DU du;

	private final ReadCodeItem readCodeItem = new ReadCodeItem();

	/**
	 * Constructor.
	 *
	 * @param du
	 *            decompilation unit
	 */
	public SmaliReader(final DU du) {
		assert du != null;

		this.du = du;
	}

	@Override
	public List<T> read(final InputStream is, final String selector) throws IOException {
		String selectorPrefix = null;
		String selectorMatch = null;
		if (selector != null && selector.endsWith(".class")) {
			selectorMatch = "L"
					+ selector.substring(selector.charAt(0) == '/' ? 1 : 0, selector.length() - 6)
					+ ";";
			final int pos = selectorMatch.lastIndexOf('/');
			if (pos != -1) {
				selectorPrefix = selectorMatch.substring(0, pos + 1);
			}
		}
		final List<T> ts = Lists.newArrayList();

		@SuppressWarnings("null")
		final byte[] bytes = ByteStreams.toByteArray(is);
		final DexFile dexFile = new DexFile(new ByteArrayInput(bytes), true, false);
		final Section<ClassDefItem> classDefItems = dexFile
				.getSectionForType(ItemType.TYPE_CLASS_DEF_ITEM);
		for (final ClassDefItem classDefItem : classDefItems.getItems()) {
			final String typeDescriptor = classDefItem.getClassType().getTypeDescriptor();
			// load full type declarations from complete package, to complex to decide here if
			// really not part of the compilation unit
			// TODO later load all type declarations, but not all bytecode details
			if (selectorPrefix != null
					&& (!typeDescriptor.startsWith(selectorPrefix) || typeDescriptor.indexOf('/',
							selectorPrefix.length()) != -1)) {
				continue;
			}
			final T t = this.du.getDescT(typeDescriptor);
			if (!t.createTd()) {
				log.warn("Type '" + t + "' already read!");
				continue;
			}
			t.setAccessFlags(classDefItem.getAccessFlags());
			final TypeIdItem superclass = classDefItem.getSuperclass();
			t.setSuperT(this.du.getDescT(superclass == null ? null : superclass.getTypeDescriptor()));
			final TypeListItem interfaces = classDefItem.getInterfaces();
			if (interfaces != null && interfaces.getTypeCount() > 0) {
				final T[] interfaceTs = new T[interfaces.getTypeCount()];
				for (int i = interfaces.getTypeCount(); i-- > 0;) {
					interfaceTs[i] = this.du.getDescT(interfaces.getTypeIdItem(i)
							.getTypeDescriptor());
				}
				t.setInterfaceTs(interfaceTs);
			}
			if (selectorMatch == null || selectorMatch.equals(typeDescriptor)) {
				ts.add(t);
			}
			A annotationDefaultValues = null;
			final Map<FieldIdItem, String> fieldSignatures = Maps.newHashMap();
			final Map<FieldIdItem, A[]> fieldAs = Maps.newHashMap();
			final Map<MethodIdItem, T[]> methodThrowsTs = Maps.newHashMap();
			final Map<MethodIdItem, String> methodSignatures = Maps.newHashMap();
			final Map<MethodIdItem, A[]> methodAs = Maps.newHashMap();
			final Map<MethodIdItem, A[][]> methodParamAs = Maps.newHashMap();

			final AnnotationDirectoryItem annotations = classDefItem.getAnnotations();
			if (annotations != null) {
				final AnnotationSetItem classAnnotations = annotations.getClassAnnotations();
				if (classAnnotations != null) {
					final List<A> as = Lists.newArrayList();
					for (final AnnotationItem annotation : classAnnotations.getAnnotations()) {
						final A a = readAnnotation(annotation);
						if ("dalvik.annotation.AnnotationDefault".equals(a.getT().getName())) {
							// annotation default values, not encoded in
							// method annotations, but in global annotation with
							// "field name" -> value
							annotationDefaultValues = (A) a.getValueMember();
							continue;
						}
						if ("dalvik.annotation.Signature".equals(a.getT().getName())) {
							// signature, is encoded as annotation with string array value
							final Object[] signature = (Object[]) a.getValueMember();
							final StringBuilder sb = new StringBuilder();
							for (final Object element : signature) {
								sb.append(element);
							}
							t.setSignature(sb.toString());
							continue;
						}
						if ("dalvik.annotation.EnclosingClass".equals(a.getT().getName())) {
							t.setEnclosingT((ClassT) a.getValueMember());
							continue;
						}
						if ("dalvik.annotation.EnclosingMethod".equals(a.getT().getName())) {
							t.setEnclosingM((M) a.getValueMember());
							continue;
						}
						if ("dalvik.annotation.InnerClass".equals(a.getT().getName())) {
							t.setInnerInfo((String) a.getMember("name"),
									(Integer) a.getMember("accessFlags"));
							continue;
						}
						if ("dalvik.annotation.MemberClasses".equals(a.getT().getName())) {
							for (final Object v : (Object[]) a.getValueMember()) {
								((T) v).setEnclosingT(t);
							}
							continue;
						}
						as.add(a);
					}
					if (as.size() > 0) {
						t.setAs(as.toArray(new A[as.size()]));
					}
				}
				for (final FieldAnnotation fieldAnnotation : annotations.getFieldAnnotations()) {
					final List<A> as = Lists.newArrayList();
					for (final AnnotationItem annotationItem : fieldAnnotation.annotationSet
							.getAnnotations()) {
						final A a = readAnnotation(annotationItem);
						if ("dalvik.annotation.Signature".equals(a.getT().getName())) {
							// signature, is encoded as annotation
							// with string array value
							final Object[] signature = (Object[]) a.getValueMember();
							final StringBuilder sb = new StringBuilder();
							for (final Object element : signature) {
								sb.append(element);
							}
							fieldSignatures.put(fieldAnnotation.field, sb.toString());
							continue;
						}
						as.add(a);
					}
					if (as.size() > 0) {
						fieldAs.put(fieldAnnotation.field, as.toArray(new A[as.size()]));
					}
				}
				for (final MethodAnnotation methodAnnotation : annotations.getMethodAnnotations()) {
					final List<A> as = Lists.newArrayList();
					for (final AnnotationItem annotationItem : methodAnnotation.annotationSet
							.getAnnotations()) {
						final A a = readAnnotation(annotationItem);
						if ("dalvik.annotation.Signature".equals(a.getT().getName())) {
							// signature, is encoded as annotation
							// with string array value
							final Object[] signature = (Object[]) a.getValueMember();
							final StringBuilder sb = new StringBuilder();
							for (final Object element : signature) {
								sb.append(element);
							}
							methodSignatures.put(methodAnnotation.method, sb.toString());
							continue;
						} else if ("dalvik.annotation.Throws".equals(a.getT().getName())) {
							// throws, is encoded as annotation with
							// type array value
							final Object[] throwables = (Object[]) a.getValueMember();
							final T[] throwsTs = new T[throwables.length];
							for (int i = throwables.length; i-- > 0;) {
								throwsTs[i] = (T) throwables[i];
							}
							methodThrowsTs.put(methodAnnotation.method, throwsTs);
							continue;
						} else {
							as.add(a);
						}
					}
					if (as.size() > 0) {
						methodAs.put(methodAnnotation.method, as.toArray(new A[as.size()]));
					}
				}
				for (final ParameterAnnotation paramAnnotation : annotations
						.getParameterAnnotations()) {
					final AnnotationSetItem[] annotationSets = paramAnnotation.annotationSet
							.getAnnotationSets();
					final A[][] paramAss = new A[annotationSets.length][];
					for (int i = annotationSets.length; i-- > 0;) {
						final AnnotationItem[] annotationItems = annotationSets[i].getAnnotations();
						final A[] paramAs = paramAss[i] = new A[annotationItems.length];
						for (int j = annotationItems.length; j-- > 0;) {
							paramAs[j] = readAnnotation(annotationItems[j]);
						}
					}
					methodParamAs.put(paramAnnotation.method, paramAss);
				}
			}
			final StringIdItem sourceFile = classDefItem.getSourceFile();
			if (sourceFile != null) {
				t.setSourceFileName(sourceFile.getStringValue());
			}
			final ClassDataItem classData = classDefItem.getClassData();
			if (classData != null) {
				readFields(t, classData.getStaticFields(), classData.getInstanceFields(),
						fieldSignatures, classDefItem.getStaticFieldInitializers(), fieldAs);
				readMethods(t, classData.getDirectMethods(), classData.getVirtualMethods(),
						methodSignatures, methodThrowsTs, annotationDefaultValues, methodAs,
						methodParamAs);
			}
			t.resolve();
		}
		return ts;
	}

	private A readAnnotation(final AnnotationEncodedSubValue encodedValue,
			final RetentionPolicy retentionPolicy) {
		final T t = this.du.getDescT(encodedValue.annotationType.getTypeDescriptor());
		final A a = new A(t, retentionPolicy);
		final StringIdItem[] names = encodedValue.names;
		final EncodedValue[] values = encodedValue.values;
		for (int i = 0; i < names.length; ++i) {
			a.addMember(names[i].getStringValue(), readValue(values[i], this.du));
		}
		return a;
	}

	private A readAnnotation(final AnnotationItem annotationItem) {
		RetentionPolicy retentionPolicy;
		switch (annotationItem.getVisibility()) {
		case BUILD:
			retentionPolicy = RetentionPolicy.SOURCE;
			break;
		case RUNTIME:
			retentionPolicy = RetentionPolicy.RUNTIME;
			break;
		case SYSTEM:
			retentionPolicy = RetentionPolicy.CLASS;
			break;
		default:
			retentionPolicy = null;
			log.warn("Unknown annotation visibility '" + annotationItem.getVisibility().visibility
					+ "'!");
		}
		return readAnnotation(annotationItem.getEncodedAnnotation(), retentionPolicy);
	}

	private void readFields(final T t, final List<EncodedField> staticFields,
			final List<EncodedField> instanceFields,
			final Map<FieldIdItem, String> fieldSignatures,
			final EncodedArrayItem staticFieldInitializers, final Map<FieldIdItem, A[]> fieldAs) {
		// static field initializer values are packed away into a different
		// section, both arrays (encoded fields and static field values) are
		// sorted in same order, there could be less static field values if
		// not all static fields have an initializer, but there is also a
		// null value as placeholder
		final EncodedValue[] staticFieldValues = staticFieldInitializers == null ? new EncodedValue[0]
				: staticFieldInitializers.getEncodedArray().values;

		for (int i = 0; i < staticFields.size(); ++i) {
			final EncodedField encodedField = staticFields.get(i);
			final FieldIdItem field = encodedField.field;

			final F f = t.getF(field.getFieldName().getStringValue(), field.getFieldType()
					.getTypeDescriptor());
			f.createFd();

			f.setAccessFlags(encodedField.accessFlags);
			if (fieldSignatures.get(field) != null) {
				f.setSignature(fieldSignatures.get(field));
			}

			if (staticFieldValues.length > i) {
				f.setValue(readValue(staticFieldValues[i], this.du));
			}

			f.setAs(fieldAs.get(field));
		}
		for (final EncodedField encodedField : instanceFields) {
			final FieldIdItem field = encodedField.field;

			final F f = t.getF(field.getFieldName().getStringValue(), field.getFieldType()
					.getTypeDescriptor());
			f.createFd();

			f.setAccessFlags(encodedField.accessFlags);
			if (fieldSignatures.get(field) != null) {
				f.setSignature(fieldSignatures.get(field));
			}

			// there is no field initializer section for instance fields,
			// only via constructor

			f.setAs(fieldAs.get(field));
		}
	}

	private void readMethods(final T t, final List<EncodedMethod> directMethods,
			final List<EncodedMethod> virtualMethods,
			final Map<MethodIdItem, String> methodSignatures,
			final Map<MethodIdItem, T[]> methodThrowsTs, final A annotationDefaultValues,
			final Map<MethodIdItem, A[]> methodAs, final Map<MethodIdItem, A[][]> methodParamAs) {
		for (final EncodedMethod encodedMethod : directMethods) {
			final MethodIdItem method = encodedMethod.method;

			final M m = t.getM(method.getMethodName().getStringValue(), method.getPrototype()
					.getPrototypeString());
			m.createMd();

			m.setAccessFlags(encodedMethod.accessFlags);
			m.setThrowsTs(methodThrowsTs.get(method));
			m.setSignature(methodSignatures.get(method));

			// no annotation default values

			m.setAs(methodAs.get(method));
			m.setParamAss(methodParamAs.get(method));

			this.readCodeItem.initAndVisit(m, encodedMethod.codeItem);
		}
		for (final EncodedMethod encodedMethod : virtualMethods) {
			final MethodIdItem method = encodedMethod.method;

			final M m = t.getM(method.getMethodName().getStringValue(), method.getPrototype()
					.getPrototypeString());
			m.createMd();

			m.setAccessFlags(encodedMethod.accessFlags);
			m.setThrowsTs(methodThrowsTs.get(method));
			m.setSignature(methodSignatures.get(method));

			if (annotationDefaultValues != null) {
				m.setAnnotationDefaultValue(annotationDefaultValues.getMember(m.getName()));
			}

			m.setAs(methodAs.get(method));
			m.setParamAss(methodParamAs.get(method));

			this.readCodeItem.initAndVisit(m, encodedMethod.codeItem);
		}
	}

	@Nullable
	private Object readValue(final EncodedValue encodedValue, final DU du) {
		if (encodedValue instanceof AnnotationEncodedSubValue) {
			// retention unknown for annotation constant
			return readAnnotation((AnnotationEncodedValue) encodedValue, null);
		}
		if (encodedValue instanceof ArrayEncodedValue) {
			final EncodedValue[] values = ((ArrayEncodedValue) encodedValue).values;
			final Object[] objects = new Object[values.length];
			for (int i = values.length; i-- > 0;) {
				objects[i] = readValue(values[i], du);
			}
			return objects;
		}
		if (encodedValue instanceof BooleanEncodedValue) {
			return ((BooleanEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof ByteEncodedValue) {
			return ((ByteEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof CharEncodedValue) {
			return ((CharEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof DoubleEncodedValue) {
			return ((DoubleEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof EnumEncodedValue) {
			final FieldIdItem fieldidItem = ((EnumEncodedValue) encodedValue).value;
			final String desc = fieldidItem.getFieldType().getTypeDescriptor();
			final T ownerT = du.getDescT(desc);
			final F f = ownerT.getF(
					fieldidItem.getFieldName().getStringDataItem().getStringValue(), desc);
			f.setEnum();
			return f;
		}
		if (encodedValue instanceof FloatEncodedValue) {
			return ((FloatEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof IntEncodedValue) {
			return ((IntEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof LongEncodedValue) {
			return ((LongEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof MethodEncodedValue) {
			final MethodIdItem methodIdItem = ((MethodEncodedValue) encodedValue).value;
			final T t = du.getDescT(methodIdItem.getContainingClass().getTypeDescriptor());
			return t.getM(methodIdItem.getMethodName().getStringValue(), methodIdItem
					.getPrototype().getPrototypeString());
		}
		if (encodedValue instanceof NullEncodedValue) {
			return null; // placeholder in constant array
		}
		if (encodedValue instanceof ShortEncodedValue) {
			return ((ShortEncodedValue) encodedValue).value;
		}
		if (encodedValue instanceof StringEncodedValue) {
			return ((StringEncodedValue) encodedValue).value.getStringValue();
		}
		if (encodedValue instanceof TypeEncodedValue) {
			return du.getDescT(((TypeEncodedValue) encodedValue).value.getTypeDescriptor());
		}
		log.warn("Unknown encoded value type '" + encodedValue.getClass().getName() + "'!");
		return null;
	}

}