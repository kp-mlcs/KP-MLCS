# KP-MLCS
## Introduction
Multiple longest common subsequence (MLCS) mining (a classical NP-hard problem) is an important task in many fields. Numerous applications in these fields can generate very long sequences (i.e., the length of the  sequences $\geq 10^{4}$), called *big sequences*. Such big sequences present a serious challenge to existing MLCS algorithms. Although significant efforts have been made to tackle the challenge, both existing exact and approximate MLCS algorithms fail to deal with big sequences as their problem-solving model MLCS-DAG (<u>D</u>irected <u>A</u>cyclic <u>G</u>raph) is too large to be calculated due to the memory explosion. To bridge the gap, this paper first proposes a new identification and deletion strategy of different classes of non-critical points, which are the points that do not contribute to the solution on the MLCS-DAG. It then proposes a new MLCS problem-solving graph model, called KP-MLCS-DAG (<u>K</u>ey <u>P</u>oint based <u>MLCS-DAG</u>). A novel parallel MLCS algorithm, called KP-MLCS (<u>K</u>ey <u>P</u>oint based <u>MLCS</u>), is also presented, which can mine and compress all MLCSs of big sequences effectively and efficiently. Extensive experiments on both synthetic and real-world biological sequences show that the proposed algorithm KP-MLCS drastically outperforms the existing state-of-the-art algorithms in terms of efficiency and effectiveness.

In summary, the main contributions of this algorithm are:
- It reveals and verifies several serious weaknesses of the popular DOP-based *MLCS* algorithms through both theoretical analysis and experiments.
- It proposes a method to efficiently identify all non-key points in the *MLCS-DAG*, which leads to a novel method to build a much smaller MLCS-DAG, called KP-MLCS-DAG (<u>K</u>ey <u>P</u>oint based <u>MLCS-DAG</u>). This method includes a novel estimator to estimate the lower bound of the *MLCS* length, an index structure, a quick sorting method of dominant points, and a cascade backtracking deletion algorithm. KP-MLCS-DAG contains only the key points, which drastically reduces the time and space cost. It also compresses and displays all *MLCS*s without extra computation.
- Based on the *KP-MLCS-DAG* and some parallelization technique, it proposes a novel parallel *MLCS* algorithm, called *KP-MLCS*, which focuses on parallel search and calculation in the key points space of the *MLCS-DAG* to mine *MLCS*s from big sequences efficiently and effectively.
 
## Install & Run

 1. Install JDK-11;
 2. Put the data files to be processed into the 'file' folder of this project;
 3. Run the following command in a shell command window.
```
java -jar -Xmx3G kp-mlcs.jar [fileName]
```
 - [fileName]: program parameter, files should be put in 'file' Folder to be read;
