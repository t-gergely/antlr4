/*
 [The "BSD license"]
 Copyright (c) 2011 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.antlr.v4.runtime.tree.gui;

import org.abego.treelayout.*;
import org.abego.treelayout.util.DefaultConfiguration;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class TreePostScriptGenerator {
	public class VariableExtentProvide implements NodeExtentProvider<Tree> {
		@Override
		public double getWidth(Tree tree) {
			String s = getText(tree);
			return doc.getWidth(s) + nodeWidthPadding*2;
		}

		@Override
		public double getHeight(Tree tree) {
			String s = getText(tree);
			double h = doc.getLineHeight() + nodeHeightPadding*2;
			String[] lines = s.split("\n");
			return h * lines.length;
		}
	}

	protected double gapBetweenLevels = 30;
	protected double gapBetweenNodes = 10;
	protected int nodeWidthPadding = 1;  // added to left/right
	protected int nodeHeightPadding = 1; // added above/below

	protected Tree root;
	protected TreeTextProvider treeTextProvider;
	protected TreeLayout<Tree> treeLayout;

	protected PostScriptDocument doc;

	public TreePostScriptGenerator(BaseRecognizer parser, Tree root) {
		this(parser, root, "CourierNew", 11);
	}

	public TreePostScriptGenerator(BaseRecognizer parser, Tree root,
								   String fontName, int fontSize)
	{
		this.root = root;
		setTreeTextProvider(new TreeViewer.DefaultTreeTextProvider(parser));
		doc = new PostScriptDocument(fontName, fontSize);
		this.treeLayout =
			new TreeLayout<Tree>(new TreeLayoutAdaptor(root),
								 new VariableExtentProvide(),
								 new DefaultConfiguration<Tree>(gapBetweenLevels,
																gapBetweenNodes,
																Configuration.Location.Bottom));
	}

	public String getPS() {
		// generate the edges and boxes (with text)
		generateEdges(getTree().getRoot());
		for (Tree textInBox : treeLayout.getNodeBounds().keySet()) {
			generateNode(textInBox);
		}

		Dimension size = treeLayout.getBounds().getBounds().getSize();
		doc.boundingBox(size.width, size.height);
		doc.close();
		return doc.getPS();
	}

	protected void generateEdges(Tree parent) {
		if (!getTree().isLeaf(parent)) {
			Rectangle2D.Double parentBounds = getBoundsOfNode(parent);
			System.out.println("%% parent("+getText(parent)+")="+parentBounds);
			double x1 = parentBounds.getCenterX();
			double y1 = parentBounds.y;
			for (Tree child : getChildren(parent)) {
				Rectangle2D.Double childBounds = getBoundsOfNode(child);
				System.out.println("%% child("+getText(child)+")="+childBounds);
				double x2 = childBounds.getCenterX();
				double y2 = childBounds.getMaxY();
				doc.line(x1, y1, x2, y2);
				generateEdges(child);
			}
		}
	}

	protected void generateNode(Tree t) {
		// draw the text on top of the box (possibly multiple lines)
		String[] lines = getText(t).split("\n");
		Rectangle2D.Double box = getBoundsOfNode(t);
		// for debugging, turn this on to see boundingbox of nodes
		// doc.rect(box.x, box.y, box.width, box.height);
		double x = box.x+nodeWidthPadding;
		double y = box.y+nodeHeightPadding;
		for (int i = 0; i < lines.length; i++) {
			doc.text(lines[i], x, y);
			y += doc.getFontSize();
		}
	}

	protected TreeForTreeLayout<Tree> getTree() {
		return treeLayout.getTree();
	}

	protected Iterable<Tree> getChildren(Tree parent) {
		return getTree().getChildren(parent);
	}

	protected Rectangle2D.Double getBoundsOfNode(Tree node) {
		return treeLayout.getNodeBounds().get(node);
	}

	protected String getText(Tree tree) {
		return treeTextProvider.getText(tree);
	}

	public TreeTextProvider getTreeTextProvider() {
		return treeTextProvider;
	}

	public void setTreeTextProvider(TreeTextProvider treeTextProvider) {
		this.treeTextProvider = treeTextProvider;
	}

	public static void main(String[] args) {
		CommonAST t = new CommonAST(new CommonToken(1, "+"));
		CommonAST a = new CommonAST(new CommonToken(1, "expression"));
		CommonAST f = new CommonAST(new CommonToken(1, "3000"));
		CommonAST b = new CommonAST(new CommonToken(1, "*"));
		CommonAST c = new CommonAST(new CommonToken(1, "42"));
		CommonAST d = new CommonAST(new CommonToken(1, "105"));
		t.addChild(a);
		a.addChild(f);
		t.addChild(b);
		b.addChild(c);
		b.addChild(d);
		TreePostScriptGenerator psgen = new TreePostScriptGenerator(null, t, "TimesNewRoman", 11);
		System.out.println(psgen.getPS());
	}
}
