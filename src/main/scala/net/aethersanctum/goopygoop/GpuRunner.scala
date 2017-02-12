package net.aethersanctum.goopygoop

import net.aethersanctum.goopygoop.Vector3.tupleToVector3
import org.jocl.CL._
import org.jocl._

class GpuContext {
  // The platform, device type and device number
  // that will be used
  val platformIndex = 0
  val deviceType: Long = CL_DEVICE_TYPE_ALL
  val deviceIndex = 0

  // Enable exceptions and subsequently omit error checks in this sample
  CL.setExceptionsEnabled(true)

  // Obtain the number of platforms
  val numPlatformsArray = new Array[Int](1)
  clGetPlatformIDs(0, null, numPlatformsArray)
  val numPlatforms = numPlatformsArray(0)

  // Obtain a platform ID
  val platforms = new Array[cl_platform_id](numPlatforms)
  clGetPlatformIDs(platforms.length, platforms, null)
  val platform = platforms(platformIndex)

  // Initialize the context properties
  val contextProperties = new cl_context_properties()
  contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform)

  // Obtain the number of devices for the platform
  val numDevicesArray = new Array[Int](1)
  clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray)
  val numDevices = numDevicesArray(0)

  // Obtain a device ID
  val devices = new Array[cl_device_id](numDevices)
  clGetDeviceIDs(platform, deviceType, numDevices, devices, null)
  val device = devices(deviceIndex)

  // Create a context for the selected device
  val context = clCreateContext(
    contextProperties, 1, Array[cl_device_id](device),
    null, null, null)

}

class GpuRunner(rendering: Rendering, scene:Scene) {
  val constants = List(
    "#define color float3",
    "",
    "__constant double3 camera_position = { 0, 1, -10 };",
    "__constant double3 look_at = { 0, 0, 30 };",
    "__constant double3 up = { 0, 1, 0 };",
    s"__constant int screen_width = ${rendering.screenWidth};",
    s"__constant int screen_height = ${rendering.screenHeight};",
    "",
    s"__constant double  march_epsilon = ${rendering.march.epsilon};",
    s"__constant double  march_step_ratio = ${rendering.march.stepRatio};",
    s"__constant double  max_distance = ${rendering.march.maxDistance};",
    s"__constant int     max_steps = ${rendering.march.maxSteps};",
    "",
    "__constant color BLACK = { 0,0,0 };",
    "__constant color WHITE = { 1,1,1 };",
    "__constant color RED = { 1,0,0 };",
    ""
  )

  val commonFunctions = List(
    "int checkering(double3 here) {",
    "   return (here.x - floor(here.x) > 0.5 ? 1 : 0)",
    "        ^ (here.y - floor(here.y) > 0.5 ? 1 : 0)",
    "        ^ (here.z - floor(here.z) > 0.5 ? 1 : 0);",
    "}",
    ""
  )
  def hitDetectionBlock:List[String] = {
    scene.objects.flatMap(obj => {
      val dd = obj.distanceInvocation
      List(
        s"    if ((measure = $dd) < march_epsilon) { hit_target = ${obj.id}; break; }"
      )
    })
  }
  val mainFunctions = List(
    "",
    "color march(double3 starting_at, double3 ray_direction) {",
    "  color ink = BLACK;",
    "  double3 point = starting_at;",
    "  double distance = 0.0;",
    "  int step;",
    "  int hit_target = -1;",
    "  for (step=0; step < max_steps; step++) { ",
    "    double measure;"
      ) ++ hitDetectionBlock ++ List(
    "    distance += measure * march_step_ratio;",
    "    point = starting_at + ray_direction * distance;",
    "  }",
    "  if (distance == 0.0 || step >= max_steps || hit_target == -1) return BLACK;",
    "  color alt = checkering(point) ? WHITE : RED;",
    "  return alt;",
    "}",
    "",
    "__kernel void kernelMain(__global int *imageMetadata, __global float *results) {",
    "    int taskId = get_global_id(0);",
    "    int row = imageMetadata[0];",
    "    double3 look_direction = normalize(look_at - camera_position);",
    "    double3 right = cross(up, look_direction);",
    "    double3 actual_up = cross(look_direction, right);",
    "    double x_pixel = (double)taskId/screen_width - 0.5;",
    "    double y_pixel = -(double)(row-screen_height/2) / screen_width;",
    "    double3 ray = look_direction + (right * x_pixel) + (actual_up * y_pixel);",
    "    color paint = march(camera_position, normalize(ray));",
    "    int outputOffset = 3 * taskId;",
    "    results[outputOffset+0] = paint.x;",
    "    results[outputOffset+1] = paint.y;",
    "    results[outputOffset+2] = paint.z;",
    "}"
  )

  val imageMetadataArray = new Array[Int](4)
  val colorVectorsPerRow = new Array[Float](3 * rendering.screenWidth)

  def run = {
    val contextSetup = new GpuContext
    val context = contextSetup.context

    // Create a command-queue for the selected device
    val commandQueue = clCreateCommandQueue(context, contextSetup.device, 0, null)

    // Allocate the memory objects for the input- and output data
    val memObjects = new Array[cl_mem](3)
    val singleColorSize = Sizeof.cl_float * 3
    val colorArraySize = singleColorSize * rendering.screenWidth

    val imageMetadataPointer = Pointer.to(imageMetadataArray)
    val imageMetadataBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
      Sizeof.cl_int *imageMetadataArray.length, imageMetadataPointer, null)
    val resultPointer = Pointer.to(colorVectorsPerRow)
    val resultBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, colorArraySize, resultPointer, null)

    val source = constants ++
        commonFunctions ++
        SceneObjectType.registry.flatMap(_.commonDeclarations) ++
        scene.declarations ++
        mainFunctions

    source foreach println

    val kernelSource = Array[String](source.mkString("\n"))
    // Create the program from the source code
    val program = clCreateProgramWithSource(context, 1, kernelSource, null, null)

    // Build the program
    clBuildProgram(program, 0, null, null, null, null)

    // Create the kernel
    val kernel = clCreateKernel(program, "kernelMain", null)

    for (row <- 0 until rendering.screenHeight) {
      printf("\rrendering row %d/%d", row, rendering.screenHeight)

      imageMetadataArray(0) = row
      clEnqueueWriteBuffer(commandQueue, imageMetadataBuffer, CL_TRUE, 0, imageMetadataArray.length, imageMetadataPointer, 0, null, null)
      // Set the arguments for the kernel
      clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(imageMetadataBuffer))
      clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(resultBuffer))

      // Set the work-item dimensions
      val global_work_size = Array[Long](rendering.screenWidth)
      val local_work_size = Array[Long](1)

      // Execute the kernel
      clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null)

      // Read the output data
      clEnqueueReadBuffer(commandQueue, resultBuffer, CL_TRUE, 0, colorArraySize, resultPointer, 0, null, null)
      paintResults(row)
    }
    println("\nCleaning up openCL stuff")
    // Release kernel, program, and memory objects
    clReleaseMemObject(resultBuffer)
    clReleaseKernel(kernel)
    clReleaseProgram(program)
    clReleaseCommandQueue(commandQueue)
    clReleaseContext(context)
    println("\nDone")
  }

  def paintResults(row:Int): Unit = {
    for (x <- 0 until rendering.screenWidth) {
      val offset = 3 * x
      val red = colorVectorsPerRow(offset + 0)
      val green = colorVectorsPerRow(offset + 1)
      val blue = colorVectorsPerRow(offset + 2)
      rendering.setPixel(x, row, red, green, blue)
    }
  }
}

object GpuRunner {
  def main(args:Array[String]):Unit = {
    val rendering = new Rendering(1024, 768, MarchOptions.QUICK)
    val scene = Scene(
      List(
        Plane(normal = (0.0, 1.0, 0.0), offset = 0.0)
      )
    )
    val runner = new GpuRunner(rendering, scene)
    runner.run
    rendering.save("GpuRunner.png")
  }
}
