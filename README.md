pcfg_earley
===========

Grammar post-processing improvements (removed recursive rules: ",-> ,", "IP-> IP" and ".-> ."). 
Obs.: Can not remove ALL cases that has only one POS tag (e.g.: NP-> N)! The best way of overcoming the special cases is removing specifically each one of them: 
"IP-> NP", "NP-> PP", "PP-> IP", "IP-> CP", "CP-> IP", "NP-> CP", "PP-> CP" and "NP-> IP". Also, removed "(, OPEN)" and "(, CLOSE)" cases (4 rules instances): ",-> O"
and ",-> C".