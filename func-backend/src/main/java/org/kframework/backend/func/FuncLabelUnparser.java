// Copyright (c) 2014-2015 K Team. All Rights Reserved.
package org.kframework.backend.func;

import org.apache.commons.lang3.StringEscapeUtils;
import org.kframework.kil.*;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.NonCachingVisitor;

public class FuncLabelUnparser extends NonCachingVisitor {
    protected StringBuilder builder = new StringBuilder();

    public FuncLabelUnparser(Context context) {
        super(context);
    }

    public String getResult() {
        return builder.toString();
    }

    public void printLabel(Term t) {
        if (t instanceof Variable) {
            Variable v = (Variable) t;
            builder.append(v.getName());
        } else {
            KLabelConstant l = (KLabelConstant)t;
            builder.append('`');
            builder.append(l.getLabel());
            builder.append('`');
        }
    }

    public void visitNodeOrKList(Term t) {
        if (t instanceof Bracket) {
            Bracket b = (Bracket)t;
            visitNodeOrKList(b.getContent());
        } else if (t instanceof KList) {
            builder.append("`#klist`(");
            visitNestedKLists((KList)t);
            builder.append(')');
        } else {
            visitNode(t);
        }
    }

    @Override
    public Void visit(Module m, Void _void) {
        if (m.isPredefined()) {
            return null;
        }
        builder.append("module ");
        builder.append(m.getName());
        builder.append('\n');
        super.visit(m,_void);
        builder.append("endmodule\n");
        return null;
    }

    @Override
    public Void visit(Import i, Void _void) {
        builder.append("imports ");
        builder.append(i.getName());
        builder.append('\n');
        return null;
    }

    @Override
    public Void visit(Rule r, Void _void) {
        builder.append("rule ");
        this.visitNode(r.getBody());
        if (r.getRequires() != null) {
            builder.append(" when ");
            this.visitNode(r.getRequires());
        }
        builder.append('\n');
        return null;
    }

    @Override
    public Void visit(Configuration c, Void _void) {
        builder.append("configuration ");
        this.visitNode(c.getBody());
        builder.append('\n');
        return null;
    }

    @Override
    public Void visit(Variable v, Void _void) {
        builder.append(v.getName());
        if (v.isUserTyped()) {
            builder.append(':');
            builder.append(v.getSort());
        }
        return null;
    }

    public void visitNestedKLists(KList children) {
        if (!children.isEmpty()) {
            boolean first = true;
            for (Term t : children.getContents()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                visitNodeOrKList(t);
            }
        }
    }

    @Override
    public Void visit(KApp app, Void _void) {
        if (app.getLabel() instanceof Token) {
            assert ((KList)app.getChild()).isEmpty();
            this.visitNode(app.getLabel());
        } else {
            printLabel(app.getLabel());
            Term child = app.getChild();
            builder.append('(');
            if (child instanceof KList) {
                visitNestedKLists((KList)child);
            } else if (child instanceof Variable) {
                Variable klistVar = (Variable)child;
                assert (klistVar.getSort().equals(Sort.KLIST));
                builder.append(klistVar.getName());
                builder.append(":KList");
            } else {
                assert false;
            }
            builder.append(')');
        }
        return null;
    }

    @Override
    public Void visit(Token t, Void _void) {
        builder.append("#token{\"");
        builder.append(t.tokenSort());
        builder.append("\",\"");
        String s = StringEscapeUtils.escapeJava(t.value());
        builder.append(s);
        builder.append("\"}");
        return null;
    }

    @Override
    public Void visit(KSequence k , Void _void) {
        if (k.isEmpty()) {
            builder.append(".K");
        } else {
            boolean first = true;
            for (Term t : k.getContents()) {
                if (!first) {
                    builder.append(" ~> ");
                }
                first = false;
                this.visitNode(t);
            }
        }
        return null;
    }

    @Override
    public Void visit(Cell cell, Void _void) {
        builder.append('<');
        builder.append(cell.getLabel());
        builder.append('>');
        if (cell.hasLeftEllipsis()) {
            builder.append("...");
        }
        this.visitNode(cell.getContents());
        if (cell.hasRightEllipsis()) {
            builder.append("...");
        }
        builder.append("</");
        builder.append(cell.getLabel());
        builder.append('>');
        return null;
    }

    @Override
    public Void visit(Rewrite rew, Void _void) {
        // builder.append('(');
        visitNodeOrKList(rew.getLeft());
        builder.append(" => ");
        visitNodeOrKList(rew.getRight());
        // builder.append(')');
        return null;
    }

    @Override
    public Void visit(Hole h, Void _void) {
        builder.append("HOLE");
        return null;
    }

    @Override
    public Void visit(TermCons c, Void _void) {
        Production prod = c.getProduction();
        builder.append("`");
        if (prod.isListDecl() && c.getContents().size() == 2) {
            UserList list = prod.getListDecl();
            builder.append('_'+list.getSeparator()+prod.getSort()+'_');
        } else {
            builder.append(prod.getKLabel());
        }
        builder.append("`");
        builder.append('(');
        boolean listOp = false;
        for (ProductionItem i : c.getProduction().getItems()) {
            if (i instanceof NonTerminal && ((NonTerminal)i).getName().equals("KList")) {
                listOp = true;
                break;
            }
        }
        boolean first = true;
        for (Term t : c.getContents()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            if (listOp) {
                visitNodeOrKList(t);
            } else {
                this.visitNode(t);
            }
        }
        builder.append(')');
        return null;
    }

    public Void visit(Bracket b, Void _void) {
        // builder.append('(');
        this.visitNode(b.getContent());
        // builder.append(')');
        return null;
    }

    /*
    @Override
    public Void visit(KList t, Void _void) {
        builder.append("<bare KList>");
        return null;
    }
    */

    @Override
    public Void visit(KLabelConstant t, Void _void) {
        builder.append("#label{\"");
        builder.append(t.getLabel());
        builder.append("\"}");
        return null;
    }

    @Override
    public Void visit(Syntax s, Void _void) {
        return null;
    }
    @Override
    public Void visit(PriorityExtended s, Void _void) {
        return null;
    }
    @Override
    public Void visit(PriorityExtendedAssoc s, Void _void) {
        return null;
    }

    @Override
    public Void visit(Cast c, Void _void) {
        this.visitNode(c.getContent());
        if (!c.isSyntactic()) {
            if (c.getContent() instanceof Variable) {
                builder.append(':');
                builder.append(c.getSort());
            } else {
                assert false;
            }
        }
        return null;
    }

    @Override
    public Void visit(ListTerminator t, Void _void) {
        builder.append('`');
        builder.append(t.toString());
        builder.append("`()");
        return null;
    }

    @Override
    public Void visit(Bag b, Void _void) {
        boolean allCells = true;
        for (Term t : b.getContents()) {
            if (t instanceof Cell
                || t instanceof TermComment) {
                continue;
            } else if (t instanceof Variable) {
                Variable v = (Variable)t;
                if (v.getSort().equals(Sort.BAG)) {
                    continue;
                }
            } else if (t instanceof Bracket) {
                Bracket r = (Bracket)t;
                if (r.getContent() instanceof Rewrite
                    && r.getContent().getSort().equals(Sort.BAG)) {
                    continue;
                }
            }
            allCells = false;
            break;
        }
        if (allCells) {
            for (Term t : b.getContents()) {
                this.visitNode(t);
            }
        } else {
            assert false;
        }
        return null;
    }

    @Override
    public Void visit(Freezer f, Void _void) {
        builder.append("#freezer(");
        this.visitNode(f.getTerm());
        builder.append(')');
        return null;
    }

    @Override
    public Void visit(FreezerHole h, Void _void) {
        builder.append("HOLE");
        return null;
    }

    @Override
    public Void visit(Term t, Void _void) {
        assert false;
        return null;
    }
}
