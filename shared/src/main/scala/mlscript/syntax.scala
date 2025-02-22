package mlscript

import mlscript.utils._, shorthands._


// Terms

final case class Pgrm(tops: Ls[Statement]) extends PgrmOrTypingUnit with PgrmImpl

sealed abstract class Decl extends DesugaredStatement with DeclImpl
final case class Def(rec: Bool, nme: Var, rhs: Term \/ PolyType, isByname: Bool) extends Decl with Terms {
  val body: Located = rhs.fold(identity, identity)
}
final case class TypeDef(
  kind: TypeDefKind,
  nme: TypeName,
  tparams: List[TypeName],
  body: Type,
  mthDecls: List[MethodDef[Right[Term, Type]]],
  mthDefs: List[MethodDef[Left[Term, Type]]],
  positionals: Ls[Var],
) extends Decl

/**
  * Method type can be a definition or a declaration based
  * on the type parameter set. A declaration has `Type` in rhs
  * and definition has `Term` in rhs.
  *
  * @param rec indicates that the method is recursive
  * @param prt name of class to which method belongs
  * @param nme name of method
  * @param tparams list of parameters for the method if any
  * @param rhs term or type if definition and declaration respectively
  */
final case class MethodDef[RHS <: Term \/ Type](
  rec: Bool,
  parent: TypeName,
  nme: Var,
  tparams: List[TypeName],
  rhs: RHS,
) extends Located {
  val body: Located = rhs.fold(identity, identity)
  val children: Ls[Located] = nme :: body :: Nil
}

sealed abstract class TypeDefKind(val str: Str)
sealed trait ObjDefKind
case object Cls extends TypeDefKind("class") with ObjDefKind
case object Trt extends TypeDefKind("trait") with ObjDefKind
case object Als extends TypeDefKind("type alias")
case object Nms extends TypeDefKind("namespace")

sealed abstract class Term                                           extends Terms with TermImpl
sealed abstract class Lit                                            extends SimpleTerm with LitImpl
final case class Var(name: Str)                                      extends SimpleTerm with VarImpl
final case class Lam(lhs: Term, rhs: Term)                           extends Term
final case class App(lhs: Term, rhs: Term)                           extends Term
final case class Tup(fields: Ls[Opt[Var] -> Fld])                    extends Term
final case class Rcd(fields: Ls[Var -> Fld])                         extends Term
final case class Sel(receiver: Term, fieldName: Var)                 extends Term
final case class Let(isRec: Bool, name: Var, rhs: Term, body: Term)  extends Term
final case class Blk(stmts: Ls[Statement])                           extends Term with BlkImpl
final case class Bra(rcd: Bool, trm: Term)                           extends Term
final case class Asc(trm: Term, ty: Type)                            extends Term
final case class Bind(lhs: Term, rhs: Term)                          extends Term
final case class Test(trm: Term, ty: Term)                           extends Term
final case class With(trm: Term, fields: Rcd)                        extends Term
final case class CaseOf(trm: Term, cases: CaseBranches)              extends Term
final case class Subs(arr: Term, idx: Term)                          extends Term
final case class Assign(lhs: Term, rhs: Term)                        extends Term
final case class Splc(fields: Ls[Either[Term, Fld]])                 extends Term
final case class New(head: Opt[(NamedType, Term)], body: TypingUnit) extends Term // `new C(...)` or `new C(){...}` or `new{...}`
final case class If(body: IfBody, els: Opt[Term])                    extends Term
final case class TyApp(lhs: Term, targs: Ls[Type])                   extends Term
final case class Quoted(body: Term)                                  extends Term 
final case class Unquoted(body: Term)                                extends Term 

sealed abstract class IfBody extends IfBodyImpl
// final case class IfTerm(expr: Term) extends IfBody // rm?
final case class IfThen(expr: Term, rhs: Term) extends IfBody
final case class IfElse(expr: Term) extends IfBody
final case class IfLet(isRec: Bool, name: Var, rhs: Term, body: IfBody) extends IfBody
final case class IfOpApp(lhs: Term, op: Var, rhs: IfBody) extends IfBody
final case class IfOpsApp(lhs: Term, opsRhss: Ls[Var -> IfBody]) extends IfBody
final case class IfBlock(lines: Ls[IfBody \/ Statement]) extends IfBody
// final case class IfApp(fun: Term, opsRhss: Ls[Var -> IfBody]) extends IfBody

final case class Fld(mut: Bool, spec: Bool, value: Term)

sealed abstract class CaseBranches extends CaseBranchesImpl
final case class Case(pat: SimpleTerm, body: Term, rest: CaseBranches) extends CaseBranches
final case class Wildcard(body: Term) extends CaseBranches
final case object NoCases extends CaseBranches

final case class IntLit(value: BigInt)            extends Lit
final case class DecLit(value: BigDecimal)        extends Lit
final case class StrLit(value: Str)               extends Lit
final case class UnitLit(undefinedOrNull: Bool)   extends Lit

sealed abstract class SimpleTerm extends Term with SimpleTermImpl

sealed trait Statement extends StatementImpl
final case class LetS(isRec: Bool, pat: Term, rhs: Term)  extends Statement
final case class DataDefn(body: Term)                     extends Statement
final case class DatatypeDefn(head: Term, body: Term)     extends Statement

sealed trait DesugaredStatement extends Statement with DesugaredStatementImpl

sealed trait Terms extends DesugaredStatement


// Types

sealed abstract class Type extends TypeImpl

sealed trait NamedType extends Type { val base: TypeName }

sealed abstract class Composed(val pol: Bool) extends Type with ComposedImpl

final case class Union(lhs: Type, rhs: Type)             extends Composed(true)
final case class Inter(lhs: Type, rhs: Type)             extends Composed(false)
final case class Function(lhs: Type, rhs: Type)          extends Type
final case class Record(fields: Ls[Var -> Field])        extends Type
final case class Tuple(fields: Ls[Opt[Var] -> Field])    extends Type
final case class Recursive(uv: TypeVar, body: Type)      extends Type
final case class AppliedType(base: TypeName, targs: List[Type]) extends Type with NamedType
final case class Neg(base: Type)                         extends Type
final case class Rem(base: Type, names: Ls[Var])         extends Type
final case class Bounds(lb: Type, ub: Type)              extends Type
final case class WithExtension(base: Type, rcd: Record)  extends Type
final case class Splice(fields: Ls[Either[Type, Field]]) extends Type
final case class Constrained(base: Type, where: Ls[TypeVar -> Bounds]) extends Type

final case class Field(in: Opt[Type], out: Type)         extends FieldImpl

sealed abstract class NullaryType                        extends Type

case object Top                                          extends NullaryType
case object Bot                                          extends NullaryType

/** Literal type type, e.g. type `0` is a type with only one possible value `0`. */
final case class Literal(lit: Lit)                       extends NullaryType

/** Reference to an existing type with the given name. */
final case class TypeName(name: Str)                     extends NullaryType with NamedType with TypeNameImpl
final case class TypeTag (name: Str)                     extends NullaryType

final case class TypeVar(val identifier: Int \/ Str, nameHint: Opt[Str]) extends NullaryType with TypeVarImpl {
  require(nameHint.isEmpty || identifier.isLeft)
  // ^ The better data structure to represent this would be an EitherOrBoth
  override def toString: Str = identifier.fold("α" + _, identity)
}

final case class PolyType(targs: Ls[TypeName], body: Type) extends PolyTypeImpl


// New Definitions AST

final case class TypingUnit(entities: Ls[Statement]) extends PgrmOrTypingUnit with TypingUnitImpl

sealed abstract class NuDecl extends Statement with NuDeclImpl

final case class NuTypeDef(
  kind: TypeDefKind,
  nme: TypeName,
  tparams: Ls[TypeName],
  params: Tup, // the specialized parameters for that type
  parents: Ls[Term],
  body: TypingUnit
) extends NuDecl with Statement

final case class NuFunDef(
  isLetRec: Opt[Bool], // None means it's a `fun`, which is always recursive; Some means it's a `let`
  nme: Var,
  targs: Ls[TypeName],
  rhs: Term \/ PolyType,
) extends NuDecl with DesugaredStatement {
  val body: Located = rhs.fold(identity, identity)
}


sealed abstract class PgrmOrTypingUnit
