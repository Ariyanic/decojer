/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  Andr� Pankraz
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
package org.decojer.cavaj.transformer;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.code.DFlag;
import org.decojer.cavaj.util.AnnotationsDecompiler;
import org.decojer.cavaj.util.SignatureDecompiler;
import org.decojer.cavaj.util.Types;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Transform JVM Struct to AST.
 * 
 * @author Andr� Pankraz
 */
public final class TrJvmStruct2JavaAst {

	private final static Logger LOGGER = Logger.getLogger(TrJvmStruct2JavaAst.class.getName());

	@SuppressWarnings("unchecked")
	private static void decompileField(final FD fd, final CU cu) {
		final F f = fd.getF();

		if ((f.check(AF.SYNTHETIC) || fd.isSynthetic())
				&& !cu.check(DFlag.DECOMPILE_UNKNOWN_SYNTHETIC)) {
			return;
		}

		final String name = f.getName();
		final TD td = fd.getTd();

		// enum synthetic fields
		if (("$VALUES".equals(name) || "ENUM$VALUES".equals(name)) && td.getT().check(AF.ENUM)
				&& !cu.check(DFlag.IGNORE_ENUM)) {
			// could extract this field name from initializer for more robustness
			return;
		}
		if (name.startsWith("this$") && !cu.check(DFlag.START_TD_ONLY)) {
			// cache for outer none-static context
			return;
		}

		final ASTNode typeDeclaration = td.getTypeDeclaration();
		final AST ast = cu.getAst();

		final boolean isFieldEnum = f.check(AF.ENUM);

		// decompile BodyDeclaration, possible subtypes:
		// FieldDeclaration, EnumConstantDeclaration
		final BodyDeclaration fieldDeclaration;
		if (isFieldEnum) {
			fieldDeclaration = ast.newEnumConstantDeclaration();
			((EnumConstantDeclaration) fieldDeclaration).setName(ast.newSimpleName(name));
		} else {
			final VariableDeclarationFragment variableDeclarationFragment = ast
					.newVariableDeclarationFragment();
			variableDeclarationFragment.setName(ast.newSimpleName(name));
			fieldDeclaration = ast.newFieldDeclaration(variableDeclarationFragment);
		}

		// decompile deprecated Javadoc-tag if no annotation set
		if (fd.isDeprecated() && !AnnotationsDecompiler.isDeprecatedAnnotation(fd.getAs())) {
			final Javadoc newJavadoc = ast.newJavadoc();
			final TagElement newTagElement = ast.newTagElement();
			newTagElement.setTagName("@deprecated");
			newJavadoc.tags().add(newTagElement);
			fieldDeclaration.setJavadoc(newJavadoc);
		}

		// decompile annotations, add annotation modifiers before other modifiers, order preserved
		// in source code generation through Eclipse JDT
		if (fd.getAs() != null) {
			AnnotationsDecompiler
					.decompileAnnotations(td, fieldDeclaration.modifiers(), fd.getAs());
		}

		// decompile modifier flags, public is default for enum and interface
		if (f.check(AF.PUBLIC)
				&& !isFieldEnum
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
		if (f.check(AF.PRIVATE)) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		}
		if (f.check(AF.PROTECTED)) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		}
		// static is default for enum and interface
		if (f.check(AF.STATIC)
				&& !isFieldEnum
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		}
		// final is default for enum and interface
		if (f.check(AF.FINAL)
				&& !isFieldEnum
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		}
		if (f.check(AF.VOLATILE)) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));
		}
		if (f.check(AF.TRANSIENT)) {
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.TRANSIENT_KEYWORD));
		}

		// not for enum constant declaration
		if (fieldDeclaration instanceof FieldDeclaration) {
			if (f.getSignature() != null) {
				new SignatureDecompiler(td, null, f.getSignature())
						.decompileFieldType((FieldDeclaration) fieldDeclaration);
			} else {
				((FieldDeclaration) fieldDeclaration).setType(Types.convertType(f.getValueT(), td,
						ast));
			}
			final Object value = fd.getValue();
			if (value != null) {
				// only final, non static - no arrays, class types
				final Expression expr = Types.convertLiteral(f.getValueT(), value, td, ast);
				if (expr != null) {
					final VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) ((FieldDeclaration) fieldDeclaration)
							.fragments().get(0);
					variableDeclarationFragment.setInitializer(expr);
				}
			}
		}
		fd.setFieldDeclaration(fieldDeclaration);
	}

	@SuppressWarnings("unchecked")
	private static void decompileMethod(final MD md, final CU cu) {
		final M m = md.getM();

		if ((m.check(AF.SYNTHETIC) || md.isSynthetic())
				&& !cu.check(DFlag.DECOMPILE_UNKNOWN_SYNTHETIC)) {
			return;
		}

		final String name = m.getName();
		final TD td = md.getTd();
		final T t = td.getT();

		// enum synthetic methods
		if (("values".equals(name) && m.getParams() == 0 || "valueOf".equals(name)
				&& m.getParams() == 1 && m.getParamT(0).is(String.class))
				&& t.check(AF.ENUM) && !cu.check(DFlag.IGNORE_ENUM)) {
			return;
		}

		final ASTNode typeDeclaration = td.getTypeDeclaration();
		final AST ast = cu.getAst();

		// decompile BodyDeclaration, possible subtypes:
		// MethodDeclaration (method or constructor),
		// AnnotationTypeMemberDeclaration (all methods in @interface) or
		// Initializer (static {})
		final BodyDeclaration methodDeclaration;
		if ("<clinit>".equals(name)) {
			// this is the static initializer "static {}" => Initializer
			methodDeclaration = ast.newInitializer();
		} else if ("<init>".equals(name)) {
			// this is the constructor => MethodDeclaration with type declaration name as name
			methodDeclaration = ast.newMethodDeclaration();
			((MethodDeclaration) methodDeclaration).setConstructor(true);
			((MethodDeclaration) methodDeclaration).setName(ast.newSimpleName(cu
					.check(DFlag.START_TD_ONLY) ? t.getPName() : t.getIName()));
		} else if (typeDeclaration instanceof AnnotationTypeDeclaration) {
			// AnnotationTypeMemberDeclaration
			methodDeclaration = ast.newAnnotationTypeMemberDeclaration();
			((AnnotationTypeMemberDeclaration) methodDeclaration).setName(ast.newSimpleName(name));

			// check if default value (byte byteTest() default 2;)
			if (md.getAnnotationDefaultValue() != null) {
				final Expression expression = AnnotationsDecompiler
						.decompileAnnotationDefaultValue(td, md.getAnnotationDefaultValue());
				if (expression != null) {
					((AnnotationTypeMemberDeclaration) methodDeclaration).setDefault(expression);
				}
			}
		} else {
			// MethodDeclaration
			methodDeclaration = ast.newMethodDeclaration();
			((MethodDeclaration) methodDeclaration).setName(ast.newSimpleName(name));
		}

		// decompile deprecated Javadoc-tag if no annotation set
		if (md.isDeprecated() && !AnnotationsDecompiler.isDeprecatedAnnotation(md.getAs())) {
			final Javadoc newJavadoc = ast.newJavadoc();
			final TagElement newTagElement = ast.newTagElement();
			newTagElement.setTagName("@deprecated");
			newJavadoc.tags().add(newTagElement);
			methodDeclaration.setJavadoc(newJavadoc);
		}

		// decompile annotations,
		// add annotation modifiers before other modifiers, order preserved in
		// source code generation through Eclipse JDT
		if (md.getAs() != null) {
			AnnotationsDecompiler.decompileAnnotations(td, methodDeclaration.modifiers(),
					md.getAs());
		}

		// decompile modifier flags,
		// public is default for interface and annotation type declarations
		if (m.check(AF.PUBLIC)
				&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
		if (m.check(AF.PRIVATE)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		}
		if (m.check(AF.PROTECTED)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		}
		if (m.check(AF.STATIC)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		}
		if (m.check(AF.FINAL)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		}
		if (m.check(AF.SYNCHRONIZED)) {
			methodDeclaration.modifiers()
					.add(ast.newModifier(ModifierKeyword.SYNCHRONIZED_KEYWORD));
		}
		if (m.check(AF.BRIDGE)) {
			// TODO
		}
		if (m.check(AF.NATIVE)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.NATIVE_KEYWORD));
		}
		// abstract is default for interface and annotation type declarations
		if (m.check(AF.ABSTRACT)
				&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
				&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
						.isInterface())) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));
		}
		if (m.check(AF.STRICTFP)) {
			methodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STRICTFP_KEYWORD));
		}
		/*
		 * AF.CONSTRUCTOR, AF.DECLARED_SYNCHRONIZED nothing, Dalvik only?
		 */
		if (methodDeclaration instanceof MethodDeclaration) {
			new SignatureDecompiler(td, m.getDescriptor(), m.getSignature()).decompileMethodParams(
					(MethodDeclaration) methodDeclaration, md);
			if (!m.check(AF.ABSTRACT) && !m.check(AF.NATIVE)) {
				// create method block for valid syntax, abstract and native methods have none
				final Block block = ast.newBlock();
				((MethodDeclaration) methodDeclaration).setBody(block);
				if (md.getCfg() != null) {
					// could have no CFG because of empty or incomplete read code attribute
					md.getCfg().setBlock(block);
				}
			}
		} else if (methodDeclaration instanceof Initializer) {
			// Initializer (static{}) has block per default
			assert ((Initializer) methodDeclaration).getBody() != null;

			if (md.getCfg() != null) {
				// could have no CFG because of empty or incomplete read code attribute
				md.getCfg().setBlock(((Initializer) methodDeclaration).getBody());
			}
		} else if (methodDeclaration instanceof AnnotationTypeMemberDeclaration) {
			final SignatureDecompiler signatureDecompiler = new SignatureDecompiler(td,
					m.getDescriptor(), m.getSignature());
			// should be empty, skip "()"
			final List<Type> methodParameterTypes = signatureDecompiler
					.decompileMethodParameterTypes();

			assert methodParameterTypes.size() == 0 : methodParameterTypes;

			final Type returnType = signatureDecompiler.decompileType();
			if (returnType != null) {
				((AnnotationTypeMemberDeclaration) methodDeclaration).setType(returnType);
			}
		}
		md.setMethodDeclaration(methodDeclaration);
	}

	/**
	 * Transform type declaration.
	 * 
	 * @param td
	 *            type declaration
	 */
	@SuppressWarnings("unchecked")
	public static void transform(final TD td) {
		final T t = td.getT();
		final CU cu = td.getCu();

		if ("package-info".equals(t.getPName())) {
			// this is not a valid Java type name and is used for package annotations, we must
			// handle this here, is "interface" in JDK 5, is "abstract synthetic interface" in JDK 7
			if (!t.check(AF.INTERFACE)) {
				LOGGER.warning("Type declaration with name 'package-info' is not an interface!");
			}
			if (td.getAs() != null) {
				AnnotationsDecompiler.decompileAnnotations(td, cu.getCompilationUnit().getPackage()
						.annotations(), td.getAs());
			}
			return;
		}
		if ((t.check(AF.SYNTHETIC) || td.isSynthetic())
				&& !cu.check(DFlag.DECOMPILE_UNKNOWN_SYNTHETIC)) {
			return;
		}

		final AST ast = cu.getAst();

		if (td.getTypeDeclaration() == null) {
			AbstractTypeDeclaration typeDeclaration = null;

			// annotation type declaration
			if (t.check(AF.ANNOTATION)) {
				if (t.getSuperT() == null || !t.getSuperT().is(Object.class)) {
					LOGGER.warning("Classfile with AccessFlag.ANNOTATION has no super class '"
							+ Object.class.getName() + "' but has '" + t.getSuperT() + "'!");
				}
				if (t.getInterfaceTs().length != 1 || !t.getInterfaceTs()[0].is(Annotation.class)) {
					LOGGER.warning("Classfile with AccessFlag.ANNOTATION has no interface '"
							+ Annotation.class.getName() + "' but has '" + t.getInterfaceTs()[0]
							+ "'!");
				}
				typeDeclaration = ast.newAnnotationTypeDeclaration();
			}
			// enum declaration
			if (t.check(AF.ENUM)) {
				if (typeDeclaration != null) {
					LOGGER.warning("Enum declaration cannot be an annotation type declaration! Ignoring.");
				} else {
					if (t.getSuperT() == null || !t.getSuperT().is(Enum.class)) {
						LOGGER.warning("Classfile with AccessFlag.ENUM has no super class '"
								+ Enum.class.getName() + "' but has '" + t.getSuperT() + "'!");
					}
					typeDeclaration = ast.newEnumDeclaration();
					// enums cannot extend other classes than Enum.class, but can have interfaces
					if (t.getInterfaceTs() != null) {
						for (final T interfaceT : t.getInterfaceTs()) {
							((EnumDeclaration) typeDeclaration).superInterfaceTypes().add(
									ast.newSimpleType(td.newTypeName(interfaceT)));
						}
					}
				}
			}
			// no annotation type declaration or enum declaration => normal class or interface type
			// declaration
			if (typeDeclaration == null) {
				typeDeclaration = ast.newTypeDeclaration();

				final SignatureDecompiler signatureDecompiler = new SignatureDecompiler(td, "L"
						+ t.getSuperT().getName() + ";", t.getSignature());
				signatureDecompiler.decompileClassTypes((TypeDeclaration) typeDeclaration,
						t.getInterfaceTs());
			}
			td.setTypeDeclaration(typeDeclaration);

			// add annotation modifiers before other modifiers, order preserved in source code
			// generation through eclipse.jdt
			if (td.getAs() != null) {
				AnnotationsDecompiler.decompileAnnotations(td, typeDeclaration.modifiers(),
						td.getAs());
			}

			// decompile remaining modifier flags
			if (t.check(AF.PUBLIC)) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			}
			if (t.check(AF.FINAL) && !(typeDeclaration instanceof EnumDeclaration)) {
				// enum declaration is final by default
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
			}
			if (t.check(AF.INTERFACE)) {
				if (typeDeclaration instanceof TypeDeclaration) {
					((TypeDeclaration) typeDeclaration).setInterface(true);
				}
			} else if (!t.check(AF.SUPER) && !td.isDalvik()) {
				// modern invokesuper syntax, is always set in current JVM, but not in Dalvik
				LOGGER.warning("Modern invokesuper syntax flag not set in type '" + td + "'!");
			}
			if (t.check(AF.ABSTRACT)
					&& !(typeDeclaration instanceof AnnotationTypeDeclaration)
					&& !(typeDeclaration instanceof TypeDeclaration && ((TypeDeclaration) typeDeclaration)
							.isInterface())) {
				typeDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));
			}

			// multiple CompilationUnit.TypeDeclaration in same AST (source file) possible, but only
			// one of them is public and multiple class files are necessary
			typeDeclaration.setName(ast.newSimpleName(cu.check(DFlag.START_TD_ONLY) ? t.getPName()
					: t.getIName()));

			if (td.isDeprecated() && !AnnotationsDecompiler.isDeprecatedAnnotation(td.getAs())) {
				final Javadoc newJavadoc = ast.newJavadoc();
				final TagElement newTagElement = ast.newTagElement();
				newTagElement.setTagName("@deprecated");
				newJavadoc.tags().add(newTagElement);
				typeDeclaration.setJavadoc(newJavadoc);
			}
		}

		final List<BD> bds = td.getBds();
		// no foreach, concurrent modification through found inner classes possible
		for (int i = 0; i < bds.size(); ++i) {
			final BD bd = bds.get(i);
			if (bd instanceof FD) {
				decompileField((FD) bd, cu);
			}
			if (bd instanceof MD) {
				decompileMethod((MD) bd, cu);
			}
		}
	}

}