/*
 *  Copyright 2017 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.twosigma.beakerx.sql.kernel;

import static com.twosigma.beakerx.DefaultJVMVariables.IMPORTS;
import static com.twosigma.beakerx.kernel.Utils.uuid;
import static com.twosigma.beakerx.sql.magic.command.DataSourcesMagicCommand.DATASOURCES;
import static com.twosigma.beakerx.sql.magic.command.DefaultDataSourcesMagicCommand.DEFAULT_DATASOURCE;

import com.twosigma.beakerx.DefaultJVMVariables;
import com.twosigma.beakerx.NamespaceClient;
import com.twosigma.beakerx.evaluator.Evaluator;
import com.twosigma.beakerx.handler.KernelHandler;
import com.twosigma.beakerx.kernel.CacheFolderFactory;
import com.twosigma.beakerx.kernel.CloseKernelAction;
import com.twosigma.beakerx.kernel.CustomMagicCommandsFactory;
import com.twosigma.beakerx.kernel.Kernel;
import com.twosigma.beakerx.kernel.KernelConfigurationFile;
import com.twosigma.beakerx.kernel.EvaluatorParameters;
import com.twosigma.beakerx.kernel.KernelFunctionality;
import com.twosigma.beakerx.kernel.KernelRunner;
import com.twosigma.beakerx.kernel.KernelSocketsFactory;
import com.twosigma.beakerx.kernel.KernelSocketsFactoryImpl;
import com.twosigma.beakerx.kernel.magic.command.MagicCommandType;
import com.twosigma.beakerx.kernel.handler.CommOpenHandler;
import com.twosigma.beakerx.message.Message;
import com.twosigma.beakerx.sql.evaluator.SQLEvaluator;
import com.twosigma.beakerx.sql.handlers.SQLCommOpenHandler;
import com.twosigma.beakerx.sql.handlers.SQLKernelInfoHandler;
import com.twosigma.beakerx.sql.magic.command.DataSourcesMagicCommand;
import com.twosigma.beakerx.sql.magic.command.DefaultDataSourcesMagicCommand;
import com.twosigma.beakerx.util.BeakerXSystem;
import com.twosigma.beakerx.util.BeakerXSystemImpl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class SQL extends Kernel {

  private final static Logger logger = Logger.getLogger(SQL.class.getName());
  public static final String BEAKERX_SQL_DEFAULT_JDBC = "BEAKERX_SQL_DEFAULT_JDBC";

  private SQL(String sessionId, Evaluator evaluator, KernelSocketsFactory kernelSocketsFactory) {
    super(sessionId, evaluator, kernelSocketsFactory, new SQLCustomMagicCommandsImpl());
  }

  public SQL(String sessionId, Evaluator evaluator, KernelSocketsFactory kernelSocketsFactory, CloseKernelAction closeKernelAction, CacheFolderFactory cacheFolderFactory) {
    super(sessionId, evaluator, kernelSocketsFactory, closeKernelAction, cacheFolderFactory, new SQLCustomMagicCommandsImpl());
  }

  @Override
  public CommOpenHandler getCommOpenHandler(Kernel kernel) {
    return new SQLCommOpenHandler(kernel);
  }

  @Override
  public KernelHandler<Message> getKernelInfoHandler(Kernel kernel) {
    return new SQLKernelInfoHandler(kernel);
  }

  public static void main(final String[] args) {
    KernelRunner.run(() -> {
      String id = uuid();
      KernelConfigurationFile configurationFile = new KernelConfigurationFile(args);
      KernelSocketsFactoryImpl kernelSocketsFactory = new KernelSocketsFactoryImpl(
              configurationFile);
      EvaluatorParameters params = getKernelParameters(new BeakerXSystemImpl());
      SQLEvaluator evaluator = new SQLEvaluator(id, id, params,NamespaceClient.create(id, configurationFile));
      return new SQL(id, evaluator, kernelSocketsFactory);
    });
  }


  static EvaluatorParameters getKernelParameters(BeakerXSystem beakerXSystem) {
    Map<String, Object> kernelParameters = new HashMap<>();
    kernelParameters.put(IMPORTS, new DefaultJVMVariables().getImports());
    kernelParameters = getDefaultConnectionString(beakerXSystem, kernelParameters);
    return new EvaluatorParameters(kernelParameters);
  }

  private static Map<String, Object> getDefaultConnectionString(BeakerXSystem beakerXSystem, Map<String, Object> kernelParameters) {
    String uri = beakerXSystem.getenv(BEAKERX_SQL_DEFAULT_JDBC);
    if (uri != null && uri.contains("jdbc:")) {
      logger.info("Setting default connection string to " + uri);
      kernelParameters.put(DEFAULT_DATASOURCE, uri);
    } else if (uri != null) {
      logger.warning("Ignoring incorrectly formatted " + BEAKERX_SQL_DEFAULT_JDBC + ": " + uri);
    }
    return kernelParameters;
  }


  static class SQLCustomMagicCommandsImpl implements CustomMagicCommandsFactory {
    @Override
    public List<MagicCommandType> customMagicCommands(KernelFunctionality kernel) {
      return Arrays.asList(
              new MagicCommandType(
                      DATASOURCES,
                      "<jdbc:[dbEngine]:[subsubprotocol:][databaseName]>",
                      new DataSourcesMagicCommand(kernel)),
              new MagicCommandType(
                      DEFAULT_DATASOURCE,
                      "<sourceName=jdbc:[dbEngine]:[subsubprotocol:][databaseName]>",
                      new DefaultDataSourcesMagicCommand(kernel)));
    }
  }

}
