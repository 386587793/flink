/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flink.streaming.util;

import java.io.File;
import java.net.URI;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.runtime.fs.hdfs.HadoopFileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Utility for copying from a HDFS {@link FileSystem} to the local file system.
 */
public class HDFSCopyToLocal {

	public static void copyToLocal(final URI remotePath,
			final File localPath) throws Exception {
		// Do it in another Thread because HDFS can deadlock if being interrupted while copying
		String threadName = "HDFS Copy from " + remotePath + " to " + localPath;

		final Tuple1<Exception> asyncException = Tuple1.of(null);

		Thread copyThread = new Thread(threadName) {
			@Override
			public void run() {
				try {
					Configuration hadoopConf = HadoopFileSystem.getHadoopConfiguration();

					FileSystem fs = FileSystem.get(remotePath, hadoopConf);
					fs.copyToLocalFile(new Path(remotePath), new Path(localPath.getAbsolutePath()));
				} catch (Exception t) {
					asyncException.f0 = t;
				}
			}
		};

		copyThread.setDaemon(true);
		copyThread.start();
		copyThread.join();

		if (asyncException.f0 != null) {
			throw asyncException.f0;
		}
	}
}
