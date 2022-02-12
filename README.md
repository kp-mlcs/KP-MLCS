# KP-MLCS
## Introduction
Multiple longest common subsequence (MLCS) mining (a classical NP-hard problem) is an important task in many fields. Numerous applications in these fields can generate very long sequences (i.e., the length of the sequences >= 10^4), called big sequences. Such big sequences present a serious challenge to existing MLCS algorithms. Although significant efforts have been made to tackle the challenge, both existing exact and approximate MLCS algorithms fail to deal with the big sequences as their problem-solving MLCS-DAG (Directed Acyclic Graph) models are too large to be calculated due to the memory explosion. To bridge the gap, this paper first proposes a new identification and deletion strategy of non-key points, which are the points that do not contribute to the solution on the MLCS-DAG. It then proposes a new MLCS problem-solving graph model, called key-point based MLCS-DAG. Based on these, a novel parallel MLCS algorithm, called KP-MLCS, is proposed, which can mine and compress all MLCSs of big sequences effectively and efficiently. Extensive experiments on both synthetic and real-world biological sequences show that the proposed algorithm KP-MLCS drastically outperforms the existing state-of-the-art algorithms in terms of efficiency and effectiveness.

In summary, the main contributions of this algorithm are:
 - It reveals and verifies several serious weaknesses of the popular DOP-based MLCS algorithms through both theoretical analysis and experiments.
 - It proposes a method to efficiently identify all non-key points in the MLCS-DAG, which leads to a novel method to build a much smaller MLCS-DAG, called KP-MLCS-DAG (key-point based MLCS-DAG). This method includes an estimation method to estimate the MLCS length, an index tree data structure, a quick sorting method of dominant points, and the cascade backtracking deletion algorithm. KP-MLCS-DAG contains only the key points, which drastically reduces the time and space cost. It also compresses and displays all MLCSs without extra computation.
 - Based on the KP-MLCS-DAG and some parallelization technique, it proposes a novel parallel MLCS algorithm, called KP-MLCS, which focuses on parallel search and calculation in the key points space of the MLCS-DAG to mine MLCSs from big sequences efficiently and effectively.
 - Extensive experiments on both synthetic and real-world biological sequence datasets demonstrate the KP-MLCS's overwhelming  advantage in the mining MLCSs from big sequences compared to the state-of-the-art existing MLCS algorithms.
## Install & Run

 1. Install JDK-11;
 2. Put the data files to be processed into the 'file' folder of this project;
 3. Run the following command in a shell command window.
```
java -jar -Xmx3G kp-mlcs.jar [fileName]
```
 - [fileName]: program parameter, files should be put in 'file' Folder to be read;
