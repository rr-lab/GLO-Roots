all: glo_roots_ms_proofing.pdf
glo_roots_ms_proofing.pdf: glo_roots_ms_proofing.md biblio_glo_roots.bib
	pandoc -V geometry:margin=1.5in -H format.sty --latex-engine=xelatex --bibliography biblio_glo_roots.bib --csl=nature.csl glo_roots_ms_proofing.md -o glo_roots_ms_proofing.pdf
