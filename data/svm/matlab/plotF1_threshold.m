% -1.00             C2W	mac-P/R/F1: 0.341/0.727/0.409 mic-P/R/F1: 0.210/0.692/0.322 TP/FP/FN: 283/1067/126 std-P/R/F1: 0.313/0.360/0.300
% -0.95             C2W	mac-P/R/F1: 0.356/0.718/0.418 mic-P/R/F1: 0.220/0.685/0.333 TP/FP/FN: 280/992/129 std-P/R/F1: 0.318/0.365/0.303
% -0.90             C2W	mac-P/R/F1: 0.362/0.716/0.424 mic-P/R/F1: 0.227/0.682/0.340 TP/FP/FN: 279/952/130 std-P/R/F1: 0.319/0.368/0.304
% -0.85             C2W	mac-P/R/F1: 0.369/0.706/0.429 mic-P/R/F1: 0.234/0.672/0.347 TP/FP/FN: 275/902/134 std-P/R/F1: 0.323/0.374/0.309
% -0.80             C2W	mac-P/R/F1: 0.377/0.685/0.432 mic-P/R/F1: 0.242/0.650/0.353 TP/FP/FN: 266/831/143 std-P/R/F1: 0.329/0.388/0.318
% -0.75             C2W	mac-P/R/F1: 0.387/0.675/0.440 mic-P/R/F1: 0.254/0.638/0.363 TP/FP/FN: 261/767/148 std-P/R/F1: 0.331/0.393/0.321
% -0.70             C2W	mac-P/R/F1: 0.405/0.640/0.446 mic-P/R/F1: 0.271/0.611/0.376 TP/FP/FN: 250/672/159 std-P/R/F1: 0.345/0.403/0.335
% -0.65             C2W	mac-P/R/F1: 0.428/0.613/0.454 mic-P/R/F1: 0.288/0.584/0.386 TP/FP/FN: 239/591/170 std-P/R/F1: 0.354/0.404/0.340
% -0.60             C2W	mac-P/R/F1: 0.452/0.580/0.457 mic-P/R/F1: 0.312/0.553/0.399 TP/FP/FN: 226/499/183 std-P/R/F1: 0.376/0.416/0.359
% -0.55             C2W	mac-P/R/F1: 0.485/0.543/0.462 mic-P/R/F1: 0.356/0.506/0.418 TP/FP/FN: 207/374/202 std-P/R/F1: 0.388/0.418/0.369
% -0.50             C2W	mac-P/R/F1: 0.531/0.517/0.464 mic-P/R/F1: 0.388/0.474/0.427 TP/FP/FN: 194/306/215 std-P/R/F1: 0.403/0.417/0.379
% -0.45             C2W	mac-P/R/F1: 0.566/0.468/0.439 mic-P/R/F1: 0.414/0.421/0.417 TP/FP/FN: 172/243/237 std-P/R/F1: 0.417/0.424/0.392
% -0.40             C2W	mac-P/R/F1: 0.599/0.441/0.426 mic-P/R/F1: 0.433/0.386/0.408 TP/FP/FN: 158/207/251 std-P/R/F1: 0.422/0.423/0.398

thresholds = -0.4:-0.05:-1;
f1 = [0.426, 0.439, 0.464, 0.462, 0.457, 0.454, 0.446, 0.440, 0.432, 0.429, 0.424, 0.418, 0.409];

plot(thresholds, f1, 'LineWidth', 2);
grid on;
xlabel('classification threshold');
ylabel('F1-score');