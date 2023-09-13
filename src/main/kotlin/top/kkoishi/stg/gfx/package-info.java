/**
 * This package contains the game's rendering and collision detection system, which can toggle whether to use
 * VRAM for double buffering, and the hardware acceleration performance of the OpenGL pipeline may be improved
 * after using VRAM, depending on the platform.
 * <p>
 * It also provides texture classes and their support, as well as a series of operations on textures, such as
 * convolution filters, affine transformations, etc.
 * <p>
 * It is worth noting that the current collision detection system only supports the following correlation algorithms
 * for intersection between shapes by default: circle, rectangle, convex polygon. Due to the complexity of concave
 * polygon correlation algorithms (for example, the division method needs to consider whether it is a
 * self-intersecting polygon, and advanced algorithms are difficult to implement), no relevant support is provided.
 * <p>
 * However, you can add support for other shapes through the methods in CollideSystem class, and you need to implement
 * it yourself, nya. :D
 * <p>
 * For the collision between two convex polygons, GJK and SAT algorithms are provided.
 * The former is based on the Minkowski difference of two convex polygons and necessarily contains the origin, while
 * the latter is based on the Separation Axis Theorem.
 */
package top.kkoishi.stg.gfx;