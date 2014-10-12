pcfg_earley
===========

Earley algoritm implemented. Jurafsky examples and running properly. Several sentences running properly to Finger's corpus, some are looping in cycle (e.g.: IP-> IP, PP-> IP, IP-> PP, NP-> PP, IP-> IP, PP-> IP, IP-> NP; happens only if the last corpus' sentence of line 1051 is maintained). Next step: FIX THIS DAMN BUG!